package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.quartz.utils.TaskWindow;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 快手订单增量采集任务（工单 06）。
 *
 * <p>按官方规格用 Unix 秒时间窗 + min_id 游标分页；订单落库复用 mw_order_dy。</p>
 */
@Slf4j
@Component
public class KsOrderCollectionTask extends OrderTask {

    private static final int DEFAULT_MINUTES = 10;

    public void run(String paramStr) {
        QueryKsOrderParam param;
        if (StringUtils.isEmpty(paramStr)) {
            param = new QueryKsOrderParam();
        } else {
            param = JSON.parseObject(paramStr, QueryKsOrderParam.class);
        }
        // 官方要求 Unix 秒级时间戳；窗口计算统一由 TaskWindow 持有
        TaskWindow.UnixWindow window = TaskWindow.unixSeconds(
                param.getStart(), param.getEnd(), DEFAULT_MINUTES);
        param.setStart(window.start());
        param.setEnd(window.end());
        saveKsOrders(param);
    }

    public void saveKsOrders(QueryKsOrderParam param) {
        log.debug("快手订单采集 start:{} end:{} minId:{}", param.getStart(), param.getEnd(), param.getMinId());
        Integer minId = saveKs(param);
        // 返回下一页游标，>当前页则继续翻页（与 EleKu/DyKu 采集任务一致）
        if (minId != null && minId > param.getMinId()) {
            param.setMinId(minId);
            saveKsOrders(param);
        }
    }
}
