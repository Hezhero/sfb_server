package com.mailvor.modules.tk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mailvor.enums.PlatformEnum;
import com.mailvor.modules.tk.domain.MailvorTbOrder;
import com.mailvor.modules.tk.domain.TkOrder;
import com.mailvor.modules.tk.vo.FoulOrderResVo;
import com.mailvor.modules.tk.vo.FoulOrderVo;
import com.mailvor.modules.user.domain.MwUserBill;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 违规订单同步服务（工单 04）。
 *
 * <p><b>业务背景</b>：淘宝联盟会对部分订单判定违规（店铺淘宝客 / 订单虚假交易）并冻结
 * 或扣除佣金。本平台此前只查正向订单，不知道哪些订单被罚，导致结算给推客的金额
 * 高于实际到账 —— 这是直接资损。本服务定期把违规清单拉下来，标记到本地订单上，
 * 让结算与账单解锁链路把冻结订单排除掉；上游解冻（{@code punish_status=1}）时再恢复。</p>
 *
 * <p><b>官方接口</b>：{@code get-foul-order-list}，文档
 * {@code https://www.dataoke.com/pmc/api-d.html?id=86}。分页是页码式
 * （{@code pageNo} 从 1 开始），查询时间跨度官方限制不超过 30 天。</p>
 *
 * <h3>订单匹配字段（重要）</h3>
 *
 * <p>官方响应里用于匹配本地订单的是 {@code tb_trade_id}（<b>子订单号</b>）。
 * 本地 {@code mw_order_tb} 有一个必须知道的反直觉之处 —— 落库时
 * {@code OrderTask.saveTb}（OrderTask.java:141-143）把父子单号<b>对调</b>了：</p>
 *
 * <pre>
 * order.setParentId(order.getTradeParentId());                   // parent_id      ← 上游 trade_parent_id（父）
 * order.setTradeParentId(Long.parseLong(order.getTradeId()));    // trade_parent_id ← 上游 trade_id（子）
 * </pre>
 *
 * <p>所以 {@code trade_parent_id} <b>这一列里存的其实是子订单号</b>，匹配必须走
 * {@link MailvorTbOrder#getTradeParentId()}。若按列名直觉去查 {@code trade_id}，
 * 会全部匹配失败且不报错 —— 静默失效，是最危险的一种错法。</p>
 *
 * <h3>与退款冲正（decHbAndRefundOrder）的关系</h3>
 *
 * <p>退款冲正由 {@code OrderRefundTask} → {@code decHbAndRefundOrder} 负责，
 * 选单依据是 {@code tk_status=13} 或 {@code refund_tag=1} 且 {@code bind=1}。
 * 本服务<b>只写 {@code foul_*} 三列</b>，绝不改 {@code bind} / {@code tk_status} /
 * {@code refund_tag}，也<b>绝不改账单状态</b>。原因是一个具体的坑：</p>
 *
 * <p>{@code decHbAndRefundOrder} 先看账单的 {@code unlock_status}：</p>
 * <ul>
 *   <li>账单仍待解锁（{@code unlock_status=1}）→ 只把账单置失效，<b>不扣用户余额</b>；</li>
 *   <li>账单已解锁（{@code unlock_status=0}）→ <b>直接从余额扣红包钱</b>。</li>
 * </ul>
 *
 * <p>如果违规逻辑抢先把待解锁账单置为失效（{@code unlock_status=2}），那么之后真实
 * 退款发生时，冲正链路会走到「账单已解锁」那一支，去扣一笔用户<b>从未到账</b>的钱 ——
 * 重复惩罚。所以违规的正确语义是<b>暂停</b>而不是<b>废弃</b>：冻结期间账单保持待解锁
 * 但不下发（见 {@link #blockedBillIds}），解冻后自然恢复解锁；若期间发生真实退款，
 * 冲正链路看到的仍是「待解锁」，行为完全不变。</p>
 */
@Slf4j
@Service
public class FoulOrderService {

    /** 单次同步最多翻多少页，防上游 total_count 异常时死循环 */
    public static final int MAX_PAGES = 50;

    /** 默认每页条数，与 {@link DataokeService#FOUL_ORDER_PAGE_SIZE} 对齐 */
    public static final int DEFAULT_PAGE_SIZE = DataokeService.FOUL_ORDER_PAGE_SIZE;

    /** 违规状态：可结算（正常 / 已解冻） */
    public static final int FOUL_STATUS_NORMAL = 0;
    /** 违规状态：冻结，不可结算 */
    public static final int FOUL_STATUS_FROZEN = 1;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private DataokeService dataokeService;

    @Resource
    private MailvorTbOrderService tbOrderService;

    // ------------------------------------------------------------------
    // 对外入口：拉取 + 落库
    // ------------------------------------------------------------------

    /**
     * 拉取指定时间窗口内的全部违规订单并标记到本地订单。
     *
     * <p>翻页策略：页码式递增，直到「本页返回条数小于 pageSize」或「上游出错/无数据」，
     * 并受 {@link #MAX_PAGES} 兜底。单条处理异常不影响整页。</p>
     *
     * @param startTime 查询开始时间，允许只给 {@code yyyy-MM-dd}
     * @param span      查询跨度（天），内部夹到官方上限 30
     * @param pageSize  每页条数，null 或非正数时用 {@link #DEFAULT_PAGE_SIZE}
     * @return 本轮实际处理的记录条数（含本地未匹配项，便于日志对账）
     */
    public int syncFoulOrders(String startTime, Integer span, Integer pageSize) {
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;
        int spanDays = DataokeService.clampFoulSpan(span);
        String normalizedStart = DataokeService.normalizeFoulStartTime(startTime);

        int handled = 0;
        for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
            FoulOrderResVo res = dataokeService.queryFoulOrderList(
                    normalizedStart, spanDays, pageNo, size, null);

            if (res == null || !res.isSuccess()) {
                // 上游故障时保留已处理的部分，本轮到此为止；下一轮会重新覆盖同一窗口
                log.warn("违规订单拉取中断 start={} span={} pageNo={} 已处理={}",
                        normalizedStart, spanDays, pageNo, handled);
                break;
            }

            List<FoulOrderVo> items = nullSafe(res.getResults());
            if (items.isEmpty()) {
                break;
            }

            for (FoulOrderVo item : items) {
                try {
                    applyFoulOrder(item);
                } catch (RuntimeException e) {
                    // 单条脏数据不能让整页失败，否则本页后面的违规订单会全部漏标
                    log.error("违规订单处理失败 tbTradeId={} err={}",
                            item == null ? null : item.getTbTradeId(), e.getMessage());
                }
            }
            handled += items.size();

            log.info("违规订单拉取 start={} span={} pageNo={} 本页={} 累计={} total={}",
                    normalizedStart, spanDays, pageNo, items.size(), handled, res.getTotalCount());

            if (items.size() < size) {
                // 未满页 = 最后一页
                break;
            }
        }
        return handled;
    }

    /** 便捷重载：默认窗口 = 最近 30 天（官方上限） */
    public int syncFoulOrders() {
        return syncFoulOrdersFromDaysAgo(DataokeService.FOUL_ORDER_MAX_SPAN_DAYS, DEFAULT_PAGE_SIZE);
    }

    /**
     * 便捷重载：从「今天往前推 daysAgo 天」开始，跨度为官方上限 30 天。
     *
     * <p>官方按订单创建时间起算，回溯时必须显式给出起点，不能只给 span。</p>
     *
     * @param daysAgo  起点距今多少天，最小 1
     * @param pageSize 每页条数
     */
    public int syncFoulOrdersFromDaysAgo(int daysAgo, Integer pageSize) {
        int days = daysAgo < 1 ? 1 : daysAgo;
        String start = LocalDate.now().minusDays(days).format(DATE);
        return syncFoulOrders(start, DataokeService.FOUL_ORDER_MAX_SPAN_DAYS, pageSize);
    }

    /**
     * 把一条官方违规记录应用到本地订单上。
     *
     * <p>幂等：重复收到同一条冻结报文不覆盖首次发现时间；解冻报文可重复下发。</p>
     *
     * @param item 官方违规记录
     * @return 是否匹配到本地订单并完成了更新
     */
    public boolean applyFoulOrder(FoulOrderVo item) {
        if (item == null || StringUtils.isBlank(item.getTbTradeId())) {
            return false;
        }
        Long tradeParentId = parseTradeId(item.getTbTradeId());
        if (tradeParentId == null) {
            // 超出 long 范围（本地主键是 bigint，本身也存不下）或非数字，跳过
            log.warn("违规订单子订单号无法转为本地主键 tbTradeId={}", item.getTbTradeId());
            return false;
        }

        MailvorTbOrder order = findLocalOrder(tradeParentId);
        if (order == null) {
            // 本地还没有这笔订单（尚未采集到 / 不属于本平台），跳过
            log.debug("违规订单在本地不存在 tbTradeId={}", item.getTbTradeId());
            return false;
        }

        if (item.isReleased()) {
            releaseOrder(order);
        } else {
            freezeOrder(order, item);
        }
        tbOrderService.updateById(order);
        return true;
    }

    // ------------------------------------------------------------------
    // 状态迁移
    // ------------------------------------------------------------------

    /** 冻结：标记不可结算。只写 foul_* 三列，不碰退款相关字段，也不动账单 */
    private void freezeOrder(MailvorTbOrder order, FoulOrderVo item) {
        boolean firstSeen = !isFoulBlocked(order);
        order.setFoulStatus(FOUL_STATUS_FROZEN);
        // 识别不出的类型不覆盖已有类型：留痕宁可旧也不能空
        Integer typeCode = item.getViolationTypeCode();
        if (typeCode != null) {
            order.setViolationType(typeCode);
        }
        if (firstSeen || order.getFoulTime() == null) {
            // 首次发现才记时间，重复报文不覆盖，便于以后追溯「什么时候被判的」
            order.setFoulTime(new Date());
        }
        log.info("违规订单已标记不可结算 tradeParentId={} violationType={}",
                order.getTradeParentId(), item.getViolationTypeText());
    }

    /** 解冻：恢复可结算。账单从未被动过，因此恢复后自然会重新进入解锁队列 */
    private void releaseOrder(MailvorTbOrder order) {
        boolean wasFrozen = isFoulBlocked(order);
        order.setFoulStatus(FOUL_STATUS_NORMAL);
        order.setFoulTime(null);
        if (wasFrozen) {
            log.info("违规订单已解冻，恢复可结算 tradeParentId={}", order.getTradeParentId());
        }
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /**
     * 按子订单号查本地订单。
     *
     * <p>匹配列是 {@code trade_parent_id}（该列实际存的是子订单号，见类注释）。</p>
     */
    private MailvorTbOrder findLocalOrder(Long tradeParentId) {
        LambdaQueryWrapper<MailvorTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MailvorTbOrder::getTradeParentId, tradeParentId);
        wrapper.last("limit 1");
        return tbOrderService.getOne(wrapper);
    }

    /**
     * 子订单号转本地主键。
     *
     * @return 非数字或超出 long 范围时返回 null
     */
    static Long parseTradeId(String tbTradeId) {
        if (StringUtils.isBlank(tbTradeId)) {
            return null;
        }
        try {
            return Long.parseLong(tbTradeId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 结算 / 解锁闸门（供 BillUnlockTask 调用）
    // ------------------------------------------------------------------

    /**
     * 该订单是否因违规被排除出结算。
     *
     * <p>只有明确 {@code foulStatus == 1} 才算冻结；null / 0 都视为正常 ——
     * 新增列之前的历史订单 {@code foul_status} 为 null，不能被误判成违规。</p>
     */
    public static boolean isFoulBlocked(TkOrder order) {
        return order != null && Objects.equals(order.getFoulStatus(), FOUL_STATUS_FROZEN);
    }

    /**
     * 从待解锁账单里找出「所属订单因违规被冻结」的那些，返回其 {@code link_id} 集合。
     *
     * <p>{@code BillUnlockTask} 用它在下发前把冻结订单的账单<b>暂缓</b>一轮：
     * 账单状态不动，解冻后下一轮自然解锁。目前只有淘宝有违规判定，
     * 因此非淘宝账单直接放行。</p>
     *
     * <p>实现上按 {@code link_id} 批量查一次，避免逐条查库。</p>
     *
     * @param bills 待解锁账单，可为 null
     * @return 需要暂缓的 bill {@code link_id}（永不为 null）
     */
    public Set<String> blockedBillIds(List<MwUserBill> bills) {
        List<MwUserBill> candidates = nullSafe(bills).stream()
                .filter(Objects::nonNull)
                .filter(b -> StringUtils.isNotBlank(b.getLinkId()))
                .filter(b -> PlatformEnum.TB.getValue().equals(b.getPlatform()))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> orderIds = new HashSet<>();
        for (MwUserBill bill : candidates) {
            Long id = parseTradeId(bill.getLinkId());
            if (id != null) {
                orderIds.add(id);
            }
        }
        if (orderIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<MailvorTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MailvorTbOrder::getTradeParentId, orderIds);
        wrapper.eq(MailvorTbOrder::getFoulStatus, FOUL_STATUS_FROZEN);
        Set<String> blocked = new HashSet<>();
        for (MailvorTbOrder order : nullSafe(tbOrderService.list(wrapper))) {
            blocked.add(String.valueOf(order.getTradeParentId()));
        }
        return blocked;
    }

    /** 列表兜底：null 转空列表，避免调用方到处判空 */
    public static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
