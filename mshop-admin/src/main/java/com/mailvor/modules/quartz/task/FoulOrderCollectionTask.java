package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.service.DataokeService;
import com.mailvor.modules.tk.service.FoulOrderService;
import com.mailvor.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 违规订单采集任务（工单 04）。
 *
 * <p>淘宝联盟判定违规（店铺淘宝客 / 订单虚假交易）并冻结或扣除佣金后，本任务把
 * 违规清单拉到本地，标记订单不可结算，避免结算金额虚高。</p>
 *
 * <p><b>时间窗口为什么不超过 30 天</b>：官方 {@code get-foul-order-list} 的参数
 * {@code span}（查询时间跨度）明确限制「不超过 30 天」。超了上游直接返回参数错误，
 * 而且报错信息不会指出是 span 的问题。因此本任务在参数解析阶段就把 span 夹到
 * [1, 30]（{@link DataokeService#clampFoulSpan}），并把 {@code day}（回溯起点）
 * 限制在 30 天以内 —— 两道保险，任一被配错都不会打到上游限制上。</p>
 *
 * <p><b>参数</b>（{@code system_quartz_job.params}，JSON）：</p>
 * <pre>
 * {"day": 7, "span": 7, "pageSize": 100}
 * day      —— 从今天往前推几天作为查询起点，默认 30，上限 30
 * span     —— 查询跨度（天），默认 7，上限 30
 * pageSize —— 每页条数，默认 100
 * </pre>
 *
 * <p><b>为什么默认只回溯 7 天</b>：违规判定通常在结算前发生，重复扫最近 30 天
 * 只是浪费上游配额。需要补历史时把 params 改成 {@code {"day":30,"span":30}}。</p>
 *
 * <p><b>注册方式</b>：{@code system_quartz_job} 里
 * {@code bean_name=com.mailvor.modules.quartz.task.FoulOrderCollectionTask} →
 * 实际是 {@code foulOrderCollectionTask}，{@code method_name=run}。
 * 注意 Quartz 反射用 {@code getDeclaredMethod}，不查父类，
 * 所以 {@code run(String)} 必须声明在<b>本类</b>上。</p>
 */
@Slf4j
@Component
public class FoulOrderCollectionTask {

    /** 官方限制：查询时间跨度不超过 30 天 */
    private static final int MAX_SPAN_DAYS = DataokeService.FOUL_ORDER_MAX_SPAN_DAYS;

    /** 回溯起点上限，与 span 上限保持一致，避免「起点太早 + 跨度太长」越过官方限制 */
    private static final int MAX_DAY = MAX_SPAN_DAYS;

    @Resource
    private FoulOrderService foulOrderService;

    /**
     * Quartz 入口。
     *
     * @param paramStr JSON 参数；为空或 {@code "{}"} 时用默认值
     */
    public void run(String paramStr) {
        FoulOrderParam param;
        if (StringUtils.isEmpty(paramStr) || "{}".equals(paramStr.trim())) {
            param = new FoulOrderParam();
        } else {
            try {
                param = JSON.parseObject(paramStr, FoulOrderParam.class);
            } catch (Exception e) {
                // Quartz 参数由运维手工录入，畸形 JSON 不应让整个任务失败；
                // 回退到默认值并记录告警，便于发现配置错误。
                log.warn("违规订单采集参数解析失败，回退默认值：{}", paramStr, e);
                param = null;
            }
            if (param == null) {
                param = new FoulOrderParam();
            }
        }
        int day = normalize(param.getDay(), MAX_DAY, MAX_DAY);
        int span = normalize(param.getSpan(), MAX_SPAN_DAYS, MAX_SPAN_DAYS);
        Integer pageSize = param.getPageSize();

        log.info("违规订单采集开始 day={} span={} pageSize={}", day, span, pageSize);
        long start = System.currentTimeMillis();
        int handled = foulOrderService.syncFoulOrdersFromDaysAgo(day, pageSize);
        log.info("违规订单采集结束 处理={} 条 耗时={}ms", handled, System.currentTimeMillis() - start);
    }

    /**
     * 收敛参数到合法区间。
     *
     * @param value   调用方传入值，null 用默认
     * @param fallback 默认值
     * @param max     上限
     */
    public static int normalize(Integer value, int fallback, int max) {
        if (value == null || value < 1) {
            return fallback;
        }
        return Math.min(value, max);
    }

    /** 任务参数 */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class FoulOrderParam {
        /** 从今天往前推几天作为查询起点（天），默认 30，上限 30 */
        private Integer day = MAX_DAY;
        /** 查询时间跨度（天），官方上限 30，默认 7 */
        private Integer span = 7;
        /** 每页条数 */
        private Integer pageSize = DataokeService.FOUL_ORDER_PAGE_SIZE;
    }
}
