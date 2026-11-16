package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.service.DtkGoodsSyncService;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 失效商品检测定时任务 —— 工单 05 链路 2/3（{@code get-stale-goods-by-time}）。
 *
 * <p><b>cron 必须至少每天一次，这是硬约束不是偏好。</b>官方对 {@code startTime}
 * 的限制是「商品下架的时间大于等于开始时间（查询时间 24 小时以内）」，即上游最多
 * 只让你回看 24 小时。如果本任务执行间隔超过 24 小时，两次执行之间下架的商品
 * 就永远不会被标记——C 端会一直展示已下架商品，正是本工单要解决的问题。
 * 当前注册 cron 为每 6 小时一次，留了 4 次冗余（机器重启、上游故障、任务补跑都能覆盖）。</p>
 *
 * <p><b>窗口与频率的关系</b>：窗口固定满 24 小时（见
 * {@link DtkGoodsSyncService#buildStaleWindow}），执行间隔取窗口的 1/4。
 * 两种走法都能保证不漏：间隔 &lt; 24h 即可，取 6h 只是让「已下架」状态更早生效。</p>
 *
 * <p><b>参数</b>（Quartz {@code params}，JSON，全部可选）：</p>
 * <pre>
 * {"pageSize":200}
 * </pre>
 */
@Slf4j
@Component
public class DtkGoodsStaleTask {

    /** 默认每页条数：失效接口官方默认 100，这里取白名单上限 200 以减少翻页次数 */
    private static final int DEFAULT_PAGE_SIZE = 200;

    @Resource
    private DtkGoodsSyncService goodsSyncService;

    /** Quartz 反射调用入口（有 params 版本） */
    public void run(String paramStr) {
        int pageSize = readPageSize(paramStr);
        log.info("失效商品检测任务开始 pageSize={} 窗口=最近24小时", pageSize);
        try {
            DtkGoodsSyncService.SyncResult result = goodsSyncService.syncStaleGoods(pageSize);
            log.info("失效商品检测任务结束 pages={} fetched={} marked={} skipped={}",
                    result.getPages(), result.getFetched(), result.getSaved(), result.getSkipped());
        } catch (RuntimeException e) {
            log.error("失效商品检测任务异常", e);
        }
    }

    /** 无参入口：Quartz 的 params 为 NULL 时走这里 */
    public void run() {
        run(null);
    }

    private static int readPageSize(String paramStr) {
        if (StringUtils.isBlank(paramStr) || !paramStr.trim().startsWith("{")) {
            return DEFAULT_PAGE_SIZE;
        }
        try {
            Integer size = JSON.parseObject(paramStr).getInteger("pageSize");
            return com.mailvor.modules.tk.param.DtkScrollPageParam.normalizePageSize(
                    size == null ? DEFAULT_PAGE_SIZE : size);
        } catch (RuntimeException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }
}
