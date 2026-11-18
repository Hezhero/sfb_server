package com.junchen.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.junchen.modules.quartz.utils.TaskWindow;
import com.junchen.modules.tk.param.QueryTbLiveOrderParam;
import com.junchen.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 淘宝更高佣订单增量采集任务（工单 08）。
 *
 * <p>按官方规格用 Unix 秒时间窗 + min_id 游标分页；订单落库复用 mw_order_tb。</p>
 */
@Slf4j
@Component
public class TbLiveOrderCollectionTask extends OrderTask {

    private static final int DEFAULT_MINUTES = 10;

    public void run(String paramStr) {
        QueryTbLiveOrderParam param;
        if (StringUtils.isEmpty(paramStr)) {
            param = new QueryTbLiveOrderParam();
        } else {
            param = JSON.parseObject(paramStr, QueryTbLiveOrderParam.class);
        }
        // 窗口计算统一由 TaskWindow 持有
        TaskWindow.UnixWindow window = TaskWindow.unixSeconds(
                param.getStartTime(), param.getEndTime(), DEFAULT_MINUTES);
        param.setStartTime(window.start());
        param.setEndTime(window.end());
        saveTbLiveOrders(param);
    }

    public void saveTbLiveOrders(QueryTbLiveOrderParam param) {
        log.debug("淘宝更高佣订单采集 start:{} end:{} minId:{}",
                param.getStartTime(), param.getEndTime(), param.getMinId());
        Integer minId = saveTbLive(param);
        if (minId != null && minId > (param.getMinId() == null ? 1 : param.getMinId())) {
            param.setMinId(minId);
            saveTbLiveOrders(param);
        }
    }
}
