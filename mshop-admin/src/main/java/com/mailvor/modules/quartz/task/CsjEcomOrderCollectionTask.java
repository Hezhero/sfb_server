package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mailvor.modules.csj.service.CsjEcomOrderService;
import com.mailvor.utils.StringUtils;
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
 * @author mailvor
 */
@Slf4j
@Component
public class CsjEcomOrderCollectionTask {

    private static final int DEFAULT_MINUTES = 10;

    @Autowired
    private CsjEcomOrderService csjEcomOrderService;

    public void run(String paramStr) {
        int minutes = DEFAULT_MINUTES;
        if (StringUtils.isNotEmpty(paramStr)) {
            try {
                JSONObject param = JSON.parseObject(paramStr);
                if (param != null && param.getInteger("minutes") != null) {
                    minutes = Math.max(param.getInteger("minutes"), 1);
                }
            } catch (Exception e) {
                log.warn("穿山甲订单采集参数解析失败，使用默认 {} 分钟: {}", DEFAULT_MINUTES, paramStr);
            }
        }
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
