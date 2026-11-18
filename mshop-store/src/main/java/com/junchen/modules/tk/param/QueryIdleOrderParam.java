package com.junchen.modules.tk.param;

import lombok.Data;

/**
 * 好单库闲鱼订单拉取参数（idle_order_list）。
 * 官方：start_date/end_date 为 Unix 秒，min_id 游标，back 最大 100。
 */
@Data
public class QueryIdleOrderParam {
    private Integer minId = 1;
    private Integer back = 100;
    private Long startTime;
    private Long endTime;
    /** 1付款(默认) 2结算 3创建 4更新 */
    private Integer dateType = 1;
    /** 0全部 1已付款 2已结算 3已退款 */
    private Integer state = 0;
}
