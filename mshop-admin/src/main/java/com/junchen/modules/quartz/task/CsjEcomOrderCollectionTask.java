package com.junchen.modules.quartz.task;

import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.csj.service.CsjEcomOrderService;
import com.junchen.modules.quartz.utils.TaskParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 穿山甲电商联盟订单采集
 *
 * <p>参数格式可空，示例：{"minutes": 30}，表示向前回溯的分钟数，默认 10 分钟。</p>
 *
 * <p>需要在后台「系统管理 → 定时任务」新增一条记录：</p>
 * <pre>
 * beanName:   csjEcomOrderCollectionTask
 * methodName: run
 * params:     {"minutes": 10}
 * cron:       0 0/10 * * * ?   （每 10 分钟一次）
 * </pre>
 *
 * @author junchen
 */
@Slf4j
@Component
public class CsjEcomOrderCollectionTask {

    private static final int DEFAULT_MINUTES = 10;

    @Autowired
    private CsjEcomOrderService csjEcomOrderService;

    public void run(String paramStr) {
        // 统一参数解析策略：坏 JSON 抛异常（ExecutionJob 记录失败并暂停任务），不再静默用默认值
        JSONObject param = TaskParams.parseObject(paramStr, "穿山甲订单采集任务");
        Integer minutesParam = param.getInteger("minutes");
        int minutes = minutesParam == null ? DEFAULT_MINUTES : Math.max(minutesParam, 1);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(minutes);
        Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());

        log.info("穿山甲订单采集开始 {} ~ {}", start, end);
        try {
            int count = csjEcomOrderService.syncOrders(startDate, endDate);
            log.info("穿山甲订单采集结束，处理 {} 条", count);
        } catch (Exception e) {
            log.error("穿山甲订单采集异常: {}", e.getMessage(), e);
        }
    }
}
