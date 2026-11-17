package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.param.QueryIdleOrderParam;
import com.mailvor.utils.OrderUtil;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 闲鱼订单增量采集任务（工单 07）。
 *
 * <p>按官方规格用 Unix 秒时间窗 + min_id 游标分页；订单落库复用 mw_order_tb。</p>
 */
@Slf4j
@Component
public class IdleOrderCollectionTask extends OrderTask {

    public void run(String paramStr) {
        QueryIdleOrderParam param;
        if (StringUtils.isEmpty(paramStr)) {
            param = new QueryIdleOrderParam();
        } else {
            param = JSON.parseObject(paramStr, QueryIdleOrderParam.class);
        }
        if (param.getStartTime() == null || param.getEndTime() == null
                || param.getStartTime() >= param.getEndTime()) {
            long end = OrderUtil.getSecondTimestamp();
            param.setEndTime(end);
            param.setStartTime(end - 10 * 60L);
        }
        saveIdleOrders(param);
    }

    public void saveIdleOrders(QueryIdleOrderParam param) {
        log.debug("闲鱼订单采集 start:{} end:{} minId:{}",
                param.getStartTime(), param.getEndTime(), param.getMinId());
        Integer minId = saveIdle(param);
        if (minId != null && minId > (param.getMinId() == null ? 1 : param.getMinId())) {
            param.setMinId(minId);
            saveIdleOrders(param);
        }
    }
}
