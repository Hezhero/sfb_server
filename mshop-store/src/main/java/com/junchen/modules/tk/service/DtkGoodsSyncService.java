package com.junchen.modules.tk.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.junchen.modules.tk.domain.MwDtkGoods;
import com.junchen.modules.tk.param.GetNewestGoodsParam;
import com.junchen.modules.tk.param.PullGoodsByTimeParam;
import com.junchen.modules.tk.param.StaleGoodsParam;
import com.junchen.modules.tk.service.mapper.MwDtkGoodsMapper;
import com.junchen.modules.tk.util.DtkScrollPager;
import com.junchen.modules.tk.vo.DtkScrollPageDataVo;
import com.junchen.modules.tk.vo.NewestGoodsVo;
import com.junchen.modules.tk.vo.PullGoodsVo;
import com.junchen.modules.tk.vo.StaleGoodsVo;
import com.junchen.utils.DateUtils;
import com.junchen.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品库同步服务（工单 05）。
 *
 * <p><b>业务背景</b>：C 端此前每屏实时调用大淘客接口，本地不落库，因此无法表达
 * 「已下架」「已变价」两种状态——用户点进去发现无法下单或价格不符，是返利类 App
 * 投诉最集中的问题。本服务把三个官方接口的结果落到本地商品库 {@code mw_dtk_goods}，
 * 并对失效商品打标记，供前端查询过滤。</p>
 *
 * <p><b>三个上游接口</b>（均已按官方文档核实字段，见各自文档页）：</p>
 * <ul>
 *   <li>{@code get-newest-goods}（id=3，v1.2.0）增量商品，字段偏少，只有价格与分类</li>
 *   <li>{@code get-stale-goods-by-time}（id=11，v1.0.1）失效商品，<b>查询窗口仅 24 小时</b></li>
 *   <li>{@code pull-goods-by-time}（id=12，v1.2.3）全量拉取，字段最全（约 50 个）</li>
 * </ul>
 *
 * <p><b>匹配键</b>：官方失效接口在响应 {@code id} 上标注「大淘客商品id（用该ID进行匹配）」，
 * 所以本地用 {@code dtk_id} 作为业务唯一键与失效匹配键。{@code goodsSign} 每次转链都会变、
 * 文档多处标注「暂时为空」，不能当键；{@code goodsId}（淘宝商品 id）是 String 且非必填，用它匹配会漏。</p>
 *
 * <p><b>容错原则</b>：定时任务是批量链路，单页/单条失败不能让整轮任务崩掉——
 * 上游异常时按「本页没有更多数据」终止翻页并记录，已拉到的数据照常落库。
 * 但「上游返回错误码」和「没有数据」必须区分，否则脏报文会静默伪装成空结果。</p>
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DtkGoodsSyncService {

    /** 单次批量写入的分片大小：避免一条 SQL 过长（MySQL max_allowed_packet） */
    private static final int BATCH_SIZE = 200;

    /** 默认翻页上限（页数）：与 DtkScrollPager 一致，防止上游异常时死循环 */
    private static final int MAX_PAGES = DtkScrollPager.DEFAULT_MAX_PAGES;

    /**
     * 失效检测单轮最多处理多少条。
     *
     * <p>24 小时窗口内下架量级通常在几千条内；设上限是为了防止上游异常返回超大结果集
     * 时一次生成过长的 {@code IN (...)}。超出的部分由下一轮任务继续处理。</p>
     */
    private static final int STALE_MARK_LIMIT = 5000;

    /** 官方时间格式 */
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * {@code mw_dtk_goods} 的列名常量。
     *
     * <p>与 {@code mshop-store/src/main/resources/sql/mw_dtk_goods.sql} 的 DDL 一一对应。
     * 写常量而不是 lambda 引用有两个好处：一是「按哪一列匹配失效」在代码里直接可见，
     * 二是这些列是稳定契约（唯一键 {@code uk_dtk_id} 依赖 {@code dtk_id}），
     * 改列名必须同时改 DDL，用常量能强制两边一起看。</p>
     */
    static final String COL_DTK_ID = "dtk_id";
    static final String COL_GOODS_STATUS = "goods_status";
    static final String COL_STALE_TIME = "stale_time";
    static final String COL_LAST_SYNC_TIME = "last_sync_time";

    @Resource
    private DataokeService dataokeService;

    @Resource
    private MwDtkGoodsMapper goodsMapper;

    /**
     * 同步结果统计。用于任务日志与运维观察「这轮到底拉了多少、写进去多少」。
     */
    @Data
    public static class SyncResult {
        /** 翻了几页（含最后那个空页） */
        private int pages;
        /** 上游返回的条数（含被丢弃的脏数据） */
        private int fetched;
        /** 实际落库/标记的条数 */
        private int saved;
        /** 被跳过的条数（缺匹配键等） */
        private int skipped;

        void addFetched(int n) {
            fetched += n;
        }

        void addSaved(int n) {
            saved += n;
        }

        void addSkipped(int n) {
            skipped += n;
        }
    }

    // ------------------------------------------------------------------
    // 5.1 商品更新 get-newest-goods
    // ------------------------------------------------------------------

    /**
     * 增量同步新商品（官方 {@code get-newest-goods}）。
     *
     * <p>按 scroll_id 分页：首次 {@code pageId=1}，后续用上一页返回的 {@code pageId}，
     * 直到返回空列表。每页落库一次，不把整批堆在内存里。</p>
     *
     * @param param    调用方给的筛选条件（可为 null，表示只按时间窗口/默认排序拉）
     * @param pageSize 每页条数，会被归一到官方白名单 10/50/100/200
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public SyncResult syncNewestGoods(GetNewestGoodsParam param, int pageSize) {
        final GetNewestGoodsParam base = param == null ? new GetNewestGoodsParam() : param;
        final SyncResult result = new SyncResult();

        result.setPages(DtkScrollPager.forEachPage((pageId, size) -> {
            base.setPageId(pageId);
            base.setPageSize(size);
            return toPage(dataokeService.newestGoods(base), NewestGoodsVo.class);
        }, pageSize, MAX_PAGES, list -> {
            result.addFetched(list.size());
            result.addSaved(upsert(list, MwDtkGoods.SOURCE_NEWEST));
        }));

        log.info("商品更新同步完成 pages={} fetched={} saved={} skipped={}",
                result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped());
        return result;
    }

    // ------------------------------------------------------------------
    // 5.2 失效商品 get-stale-goods-by-time
    // ------------------------------------------------------------------

    /**
     * 同步失效（已下架）商品并按大淘客商品 id 标记本地记录（官方 {@code get-stale-goods-by-time}）。
     *
     * <p><b>24 小时窗口</b>：官方限定 {@code startTime} 只能追溯到最近 24 小时。
     * 本方法显式把窗口收敛到 {@code [now-24h, now]}，且任务 cron 必须至少每天执行一次——
     * 这两件事缺一不可：cron 频率不够会漏数据，窗口不收敛会被上游拒绝。</p>
     *
     * <p><b>不删行只打标记</b>：失效商品保留历史记录，一旦同一 {@code dtk_id} 再次出现在
     * 增量/全量拉取里会被 upsert 改回正常状态（商品重新上架的真实场景）。</p>
     *
     * @param pageSize 每页条数，归一到官方白名单
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public SyncResult syncStaleGoods(int pageSize) {
        final StaleGoodsParam param = buildStaleWindow(new Date());
        final SyncResult result = new SyncResult();
        final Set<Long> staleIds = new HashSet<>();

        result.setPages(DtkScrollPager.forEachPage((pageId, size) -> {
            param.setPageId(pageId);
            param.setPageSize(size);
            return toPage(dataokeService.staleGoods(param), StaleGoodsVo.class);
        }, pageSize, MAX_PAGES, list -> {
            result.addFetched(list.size());
            for (StaleGoodsVo vo : list) {
                if (vo != null && vo.getId() != null) {
                    staleIds.add(vo.getId());
                } else {
                    result.addSkipped(1);
                }
            }
            // 每页都落一次标记，避免某一页异常导致整轮白跑
            result.addSaved(markStale(staleIds));
        }));

        if (!staleIds.isEmpty()) {
            // 兜底：把最后一批可能没标记到的也标记上（markStale 已幂等）
            result.addSaved(markStale(staleIds));
        }

        log.info("失效商品同步完成 pages={} fetched={} marked={} skipped={} windowStart={} windowEnd={}",
                result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped(),
                param.getStartTime(), param.getEndTime());
        return result;
    }

    /**
     * 构造失效检测的时间窗口。
     *
     * <p>官方限制 24 小时，这里固定取 {@code now-24h ~ now}：</p>
     * <ul>
     *   <li>不取更长——上游不接受，传了也会被截断或报参数错误</li>
     *   <li>不取更短——<b>任务当天只跑一次</b>时，短窗口会漏掉两次执行之间的下架记录</li>
     * </ul>
     * 这个关系是「至少每天一次」这条 cron 约束的由来，不要单方面调窄窗口。
     *
     * @param now 本轮执行时刻（由调用方传入便于测试）
     */
    public static StaleGoodsParam buildStaleWindow(Date now) {
        long end = now.getTime();
        long start = end - StaleGoodsParam.MAX_WINDOW_MILLIS;
        StaleGoodsParam param = new StaleGoodsParam();
        param.setStartTime(format(new Date(start)));
        param.setEndTime(format(new Date(end)));
        return param;
    }

    /**
     * 按大淘客商品 id 把本地记录标记为失效。
     *
     * <p>用显式列名（{@link #COL_DTK_ID} 等）而不是 MyBatis-Plus 的 lambda 引用：
     * 这些列名与本工单 DDL（{@code mw_dtk_goods.sql}）一一对应，写成常量后
     * 「失效到底按哪一列匹配」在代码里一眼可见，不必去猜实体字段映射。</p>
     *
     * @return 实际被标记的条数（已经是失效状态的不重复计入）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int markStale(Collection<Long> dtkIds) {
        if (dtkIds == null || dtkIds.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        int affected = 0;
        List<Long> all = new ArrayList<>(dtkIds);
        for (int from = 0; from < all.size(); from += BATCH_SIZE) {
            List<Long> chunk = all.subList(from, Math.min(from + BATCH_SIZE, all.size()));
            UpdateWrapper<MwDtkGoods> update = new UpdateWrapper<>();
            update.in(COL_DTK_ID, chunk)
                    // 已经是失效的不再更新，避免 stale_time 每轮被刷新、失去取证价值
                    .ne(COL_GOODS_STATUS, MwDtkGoods.STATUS_STALE)
                    .set(COL_GOODS_STATUS, MwDtkGoods.STATUS_STALE)
                    .set(COL_STALE_TIME, now)
                    .set(COL_LAST_SYNC_TIME, now);
            affected += goodsMapper.update(null, update);
        }
        return affected;
    }

    /**
     * 取当前失效商品的大淘客商品 id 列表，供 C 端查询过滤。
     *
     * @param limit 最多取多少条（防止一次捞太多）
     */
    public List<Long> listStaleDtkIds(int limit) {
        QueryWrapper<MwDtkGoods> query = new QueryWrapper<>();
        query.select(COL_DTK_ID)
                .eq(COL_GOODS_STATUS, MwDtkGoods.STATUS_STALE)
                .isNotNull(COL_DTK_ID);
        if (limit > 0) {
            query.last("limit " + limit);
        }
        List<MwDtkGoods> rows = goodsMapper.selectList(query);
        List<Long> ids = new ArrayList<>();
        if (rows != null) {
            for (MwDtkGoods row : rows) {
                if (row != null && row.getDtkId() != null) {
                    ids.add(row.getDtkId());
                }
            }
        }
        return ids;
    }

    // ------------------------------------------------------------------
    // 5.3 定时拉取 pull-goods-by-time
    // ------------------------------------------------------------------

    /**
     * 全量/按时间窗口拉取商品（官方 {@code pull-goods-by-time}）。
     *
     * <p>该接口响应字段最全（约 50 个），是本地商品库字段的唯一完整来源；
     * 券后价、佣金比例这类最容易变的字段都在这里，因此它也是「价格保鲜」的主力链路。</p>
     *
     * @param param    调用方给的筛选条件（可为 null）
     * @param pageSize 每页条数，归一到官方白名单
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public SyncResult syncPullGoods(PullGoodsByTimeParam param, int pageSize) {
        final PullGoodsByTimeParam base = param == null ? new PullGoodsByTimeParam() : param;
        final SyncResult result = new SyncResult();

        result.setPages(DtkScrollPager.forEachPage((pageId, size) -> {
            base.setPageId(pageId);
            base.setPageSize(size);
            return toPage(dataokeService.pullGoodsByTime(base), PullGoodsVo.class);
        }, pageSize, MAX_PAGES, list -> {
            result.addFetched(list.size());
            result.addSaved(upsertPull(list));
        }));

        log.info("定时拉取同步完成 pages={} fetched={} saved={} skipped={}",
                result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped());
        return result;
    }

    // ------------------------------------------------------------------
    // 内部：上游 JSON → 分页对象
    // ------------------------------------------------------------------

    /**
     * 把上游原始 JSON 转成翻页器认识的一页。
     *
     * <p>映射关系（官方「返回示例」）：{@code data.list} / {@code data.pageId}。
     * 任何异常都收敛成「空页」——包括上游返回 {@code code != 0} 的错误报文，
     * 否则错误响应会被当成「有数据但 list 为空」继续翻页，白跑满页数上限。</p>
     */
    private static <T> DtkScrollPager.Page<T> toPage(JSONObject response, Class<T> itemType) {
        if (response == null) {
            return DtkScrollPager.Page.empty();
        }
        Integer code = response.getInteger("code");
        if (code == null || code != 0) {
            return DtkScrollPager.Page.empty();
        }
        DtkScrollPageDataVo<T> data = parseData(response, itemType);
        if (data == null) {
            return DtkScrollPager.Page.empty();
        }
        return new DtkScrollPager.Page<>(data.getList(), data.getPageId());
    }

    private static <T> DtkScrollPageDataVo<T> parseData(JSONObject response, Class<T> itemType) {
        JSONObject dataJson = response.getJSONObject("data");
        if (dataJson == null) {
            return null;
        }
        DtkScrollPageDataVo<T> data = new DtkScrollPageDataVo<>();
        data.setPageId(dataJson.getString("pageId"));
        data.setTotalNum(dataJson.getLong("totalNum"));
        JSONArray array = dataJson.getJSONArray("list");
        if (array != null && !array.isEmpty()) {
            try {
                data.setList(array.toJavaList(itemType));
            } catch (RuntimeException e) {
                log.error("大淘客商品库响应解析失败 itemType={} 首个元素={}",
                        itemType.getSimpleName(), array.get(0));
                return null;
            }
        }
        return data;
    }

    // ------------------------------------------------------------------
    // 内部：落库
    // ------------------------------------------------------------------

    /** 商品更新只返回价格与分类字段，且明确不含标题/图片，落库时按「有值才覆盖」处理 */
    private int upsert(List<NewestGoodsVo> list, String source) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<MwDtkGoods> rows = new ArrayList<>(list.size());
        Date now = new Date();
        for (NewestGoodsVo vo : list) {
            if (vo == null || vo.getId() == null) {
                continue;   // 没有匹配键，入库后永远无法被失效检测命中
            }
            rows.add(MwDtkGoods.builder()
                    .dtkId(vo.getId())
                    .goodsId(vo.getGoodsId())
                    .goodsSign(blankToNull(vo.getGoodsSign()))
                    .originalPrice(vo.getOriginalPrice())
                    .actualPrice(vo.getActualPrice())
                    .couponPrice(vo.getCouponPrice())
                    .couponLink(vo.getCouponLink())
                    .couponRemainCount(vo.getCouponRemainCount())
                    .couponReceiveNum(vo.getCouponReceiveNum())
                    .commissionRate(vo.getCommissionRate())
                    .monthSales(vo.getMonthSales())
                    .subcid(joinIds(vo.getSubcid()))
                    .goodsStatus(MwDtkGoods.STATUS_NORMAL)
                    .source(source)
                    .lastSyncTime(now)
                    .build());
        }
        return persist(rows, list.size());
    }

    /** 定时拉取返回全字段，是本地商品库的主要写入来源 */
    private int upsertPull(List<PullGoodsVo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<MwDtkGoods> rows = new ArrayList<>(list.size());
        Date now = new Date();
        for (PullGoodsVo vo : list) {
            if (vo == null || vo.getId() == null) {
                continue;
            }
            rows.add(MwDtkGoods.builder()
                    .dtkId(vo.getId())
                    .goodsId(vo.getGoodsId())
                    .goodsSign(blankToNull(vo.getGoodsSign()))
                    .itemLink(vo.getItemLink())
                    .title(vo.getTitle())
                    .dtitle(vo.getDtitle())
                    .description(vo.getDesc())
                    .cid(vo.getCid())
                    .subcid(joinIds(vo.getSubcid()))
                    .mainPic(vo.getMainPic())
                    .marketingMainPic(vo.getMarketingMainPic())
                    .originalPrice(vo.getOriginalPrice())
                    .actualPrice(vo.getActualPrice())
                    .couponPrice(vo.getCouponPrice())
                    .couponLink(vo.getCouponLink())
                    .couponReceiveNum(vo.getCouponReceiveNum())
                    .commissionRate(vo.getCommissionRate())
                    .monthSales(vo.getMonthSales())
                    .shopName(vo.getShopName())
                    .sellerId(vo.getSellerId())
                    .goodsCreateTime(parseTime(vo.getCreateTime()))
                    .goodsStatus(MwDtkGoods.STATUS_NORMAL)
                    .source(MwDtkGoods.SOURCE_PULL)
                    .lastSyncTime(now)
                    .build());
        }
        return persist(rows, list.size());
    }

    /**
     * 真正写库：分片批量 upsert。
     *
     * <p>用 {@code mysqlInsertOrUpdateBath}（{@code ON DUPLICATE KEY UPDATE}）而不是
     * 「先 select 再 insert/update」：后者在并行任务下会重复插入，而且价格变更时
     * 两条链路可能互相覆盖。upsert 依赖 {@code mw_dtk_goods.dtk_id} 上的唯一索引。</p>
     */
    private int persist(List<MwDtkGoods> rows, int fetched) {
        if (rows.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            List<MwDtkGoods> chunk = rows.subList(from, Math.min(from + BATCH_SIZE, rows.size()));
            affected += goodsMapper.mysqlInsertOrUpdateBath(chunk);
        }
        if (affected == 0 && !rows.isEmpty()) {
            log.warn("大淘客商品库批量写入未生效 fetched={} 待写={}", fetched, rows.size());
        }
        return affected;
    }

    // ------------------------------------------------------------------
    // 内部：小工具
    // ------------------------------------------------------------------

    private static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    /** 官方时间字段是 {@code yyyy-MM-dd HH:mm:ss} 字符串；解析失败留空而不是抛异常 */
    static Date parseTime(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return new SimpleDateFormat(TIME_PATTERN).parse(text.trim());
        } catch (ParseException e) {
            log.debug("大淘客商品上架时间格式非预期 value={}", text);
            return null;
        }
    }

    private static String format(Date date) {
        return new SimpleDateFormat(TIME_PATTERN).format(date);
    }
}
