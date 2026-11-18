package com.junchen.modules.tk.param;

import lombok.Data;

/**
 * 好单库「淘宝更高佣订单拉取」参数（tb_live_order_list）。
 *
 * <p>官方规格：start_date/end_date 为 Unix 秒级时间戳，min_id 为游标，
 * back 最大 100；date_type 1付款/2结算/4更新（注意没有 3）。</p>
 */
@Data
public class QueryTbLiveOrderParam {
    private Integer minId = 1;
    private Integer back = 100;
    /** Unix 秒 */
    private Long startTime;
    /** Unix 秒 */
    private Long endTime;
    /** 1付款(默认) / 2结算 / 4更新 */
    private Integer dateType = 1;
    /** 0全部 1已付款 2已结算 3已退款 */
    private Integer state = 0;
}
