package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.utils.OrderUtil;
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

    public void run(String paramStr) {
        QueryKsOrderParam param;
        if (StringUtils.isEmpty(paramStr)) {
            param = new QueryKsOrderParam();
        } else {
            param = JSON.parseObject(paramStr, QueryKsOrderParam.class);
        }
        // 官方要求 Unix 秒级时间戳；默认采集最近 10 分钟，参数可覆盖。
        if (param.getStart() == null || param.getEnd() == null || param.getStart() >= param.getEnd()) {
            long end = OrderUtil.getSecondTimestamp();
            long start = end - 10 * 60L;
            param.setEnd(end);
            param.setStart(start);
        }
        saveKsOrders(param);
    }

    protected void saveKsOrders(QueryKsOrderParam param) {
        log.debug("快手订单采集 start:{} end:{} minId:{}", param.getStart(), param.getEnd(), param.getMinId());
        Integer minId = saveKs(param);
        // 返回下一页游标，>当前页则继续翻页（与 EleKu/DyKu 采集任务一致）
        if (minId != null && minId > param.getMinId()) {
            param.setMinId(minId);
            saveKsOrders(param);
        }
    }
}
