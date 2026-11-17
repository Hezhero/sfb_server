package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.utils.OrderUtil;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 快手历史订单采集任务（工单 06）：按时间窗口回溯，逐段用 min_id 游标拉全量。
 */
@Slf4j
@Component
public class KsOrderHistoryCollectionTask extends OrderTask {

    private static final long ONE_DAY_SECONDS = 24L * 3600;

    public void run(String paramStr) throws InterruptedException {
        QueryKsOrderParam param;
        if (StringUtils.isEmpty(paramStr)) {
            param = new QueryKsOrderParam();
        } else {
            param = JSON.parseObject(paramStr, QueryKsOrderParam.class);
        }
        long end = param.getEnd() == null ? OrderUtil.getSecondTimestamp() : param.getEnd();
        long start = param.getStart() == null ? end - 30 * ONE_DAY_SECONDS : param.getStart();
        // 单次窗口不超过一天，避免上游返回过多导致超时
        long window = ONE_DAY_SECONDS;

        while (end > start) {
            long segmentStart = Math.max(start, end - window);
            param.setStart(segmentStart);
            param.setEnd(end);
            param.setMinId(0);
            log.debug("快手历史订单采集 start:{} end:{}", param.getStart(), param.getEnd());
            Integer minId = saveKs(param);
            while (minId != null && minId > param.getMinId()) {
                param.setMinId(minId);
                minId = saveKs(param);
            }
            end = segmentStart;
            Thread.sleep(100);
        }
    }
}
