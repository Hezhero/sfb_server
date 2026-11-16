package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.param.GetNewestGoodsParam;
import com.mailvor.modules.tk.service.DtkGoodsSyncService;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 商品更新（增量同步）定时任务 —— 工单 05 链路 1/3（{@code get-newest-goods}）。
 *
 * <p><b>为什么高频</b>：新上架商品越早入本地库，C 端可选商品池越新鲜，也能尽快
 * 参与「新品」位展示。上游该接口按上架时间过滤，窗口只影响「增量发现」的及时性，
 * 不影响价格正确性（价格保鲜另有全量拉取链路）。所以建议每小时一次。</p>
 *
 * <p><b>时间窗口</b>：未传参数时取「最近 2 小时」——比执行频率（1 小时）宽，
 * 是为了容忍一次执行失败/机器重启：宁可重复拉一小段（upsert 幂等，不会产生重复行），
 * 也不要出现时间空洞。</p>
 *
 * <p><b>参数</b>（Quartz {@code params} 字段，JSON 字符串）：</p>
 * <pre>
 * {"minutes":120,"pageSize":200,"cids":"1,2,3","sort":"1","hasCoupon":1}
 * </pre>
 * 全部可选；不写就是「最近 2 小时 + 每页 200 + 有券」。
 */
@Slf4j
@Component
public class DtkGoodsNewestTask {

    private static final DateTimeFormatter FF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认回看分钟数：比建议执行间隔（1 小时）宽一倍，容忍单次失败 */
    private static final int DEFAULT_MINUTES = 120;

    /** 默认每页条数（官方白名单内） */
    private static final int DEFAULT_PAGE_SIZE = 200;

    @Resource
    private DtkGoodsSyncService goodsSyncService;

    /**
     * Quartz 反射调用入口：有 params 传 String，无 params 调无参版本。
     * 见 {@code QuartzRunnable} 的 getDeclaredMethod 分支。
     */
    public void run(String paramStr) {
        GetNewestGoodsParam param;
        if (StringUtils.isBlank(paramStr)) {
            param = new GetNewestGoodsParam();
        } else {
            param = JSON.parseObject(paramStr, GetNewestGoodsParam.class);
        }
        if (param == null) {
            param = new GetNewestGoodsParam();
        }

        Integer minutes = readMinutes(paramStr);
        if (StringUtils.isBlank(param.getStartTime()) || StringUtils.isBlank(param.getEndTime())) {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusMinutes(minutes == null ? DEFAULT_MINUTES : minutes);
            param.setStartTime(start.format(FF));
            param.setEndTime(end.format(FF));
        }
        if (param.getHasCoupon() == null) {
            // 官方默认 1（有券）；显式写上，避免上游默认值与本地预期不一致
            param.setHasCoupon(1);
        }

        int pageSize = readPageSize(paramStr, DEFAULT_PAGE_SIZE);
        log.info("商品更新任务开始 start={} end={} pageSize={}",
                param.getStartTime(), param.getEndTime(), pageSize);
        try {
            DtkGoodsSyncService.SyncResult result = goodsSyncService.syncNewestGoods(param, pageSize);
            log.info("商品更新任务结束 pages={} fetched={} saved={} skipped={}",
                    result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped());
        } catch (RuntimeException e) {
            // 定时任务不能让异常穿透到 Quartz：否则下一轮调度会按 misfire 处理，越拖越乱
            log.error("商品更新任务异常", e);
        }
    }

    /** 无参入口：Quartz 的 params 为 NULL 时走这里 */
    public void run() {
        run(null);
    }

    /** 从原始 JSON 里取 minutes（不在 GetNewestGoodsParam 里，属于任务级调度参数） */
    private static Integer readMinutes(String paramStr) {
        if (StringUtils.isBlank(paramStr) || !paramStr.trim().startsWith("{")) {
            return null;
        }
        try {
            return JSON.parseObject(paramStr).getInteger("minutes");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int readPageSize(String paramStr, int defaultSize) {
        if (StringUtils.isBlank(paramStr) || !paramStr.trim().startsWith("{")) {
            return defaultSize;
        }
        try {
            Integer size = JSON.parseObject(paramStr).getInteger("pageSize");
            return com.mailvor.modules.tk.param.DtkScrollPageParam.normalizePageSize(
                    size == null ? defaultSize : size);
        } catch (RuntimeException e) {
            return defaultSize;
        }
    }
}
