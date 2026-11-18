package com.junchen.modules.csj.service;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 穿山甲激励视频积分发放监控。
 *
 * <p>用一个原子计数器按天累计发放总量，提供快照供运维查询。
 * 当单日发放超过配置的阈值时输出 WARN 日志——运维的日志告警系统可据此发现异常 spike。
 * 不引入新表，进程重启后计数归零（跨天也归零），适合作为「运行时熔断提示」而非财务账本。</p>
 */
@Component
@Slf4j
public class CsjRewardMonitor {

    /** 单日发放积分的告警阈值，超过则 WARN。可由配置覆盖，默认 100000。 */
    private static final long DEFAULT_DAILY_ALERT_THRESHOLD = 100_000L;

    private final AtomicLong day = new AtomicLong(LocalDate.now().toEpochDay());
    private final AtomicLong grantedIntegral = new AtomicLong();
    private final AtomicLong grantedCount = new AtomicLong();
    private volatile long alertThreshold = DEFAULT_DAILY_ALERT_THRESHOLD;
    private volatile boolean alerted = false;

    /** 记录一次发放 */
    public void record(long integral) {
        rollIfNeeded();
        grantedIntegral.addAndGet(integral);
        grantedCount.incrementAndGet();
        long total = grantedIntegral.get();
        if (!alerted && total > alertThreshold) {
            alerted = true;
            log.warn("【穿山甲积分发放异常】单日累计发放 {} 积分（{} 次），已超过阈值 {}，请检查是否被刷",
                    total, grantedCount.get(), alertThreshold);
        }
    }

    /** 当前监控快照 */
    public Snapshot snapshot() {
        rollIfNeeded();
        Snapshot s = new Snapshot();
        s.setDate(LocalDate.ofEpochDay(day.get()).toString());
        s.setGrantedCount(grantedCount.get());
        s.setGrantedIntegral(grantedIntegral.get());
        s.setAlertThreshold(alertThreshold);
        s.setAlerted(alerted);
        return s;
    }

    public void setAlertThreshold(long threshold) {
        this.alertThreshold = threshold > 0 ? threshold : DEFAULT_DAILY_ALERT_THRESHOLD;
    }

    /** 跨天归零 */
    private void rollIfNeeded() {
        long today = LocalDate.now().toEpochDay();
        if (day.get() != today && day.compareAndSet(day.get(), today)) {
            grantedIntegral.set(0);
            grantedCount.set(0);
            alerted = false;
        }
    }

    @Data
    public static class Snapshot {
        @ApiModelProperty(value = "统计日期")
        private String date;
        @ApiModelProperty(value = "发放次数")
        private long grantedCount;
        @ApiModelProperty(value = "发放积分总量")
        private long grantedIntegral;
        @ApiModelProperty(value = "告警阈值")
        private long alertThreshold;
        @ApiModelProperty(value = "是否已触发告警")
        private boolean alerted;
    }
}
