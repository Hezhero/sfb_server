package com.mailvor.modules.tk.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 快手订单拉取参数（ks_order_list）。
 *
 * <p>官方规格：min_id 为分页游标（返回里给下一页值）；back ≤ 100；
 * start_date/end_date 为 Unix 秒级时间戳；date_type 1付款/2结算/3更新；
 * state 0全部/1已付款/2已收货/3已结算/4已失效。</p>
 */
@Data
@ApiModel(value = "QueryKsOrderParam", description = "快手订单拉取参数")
public class QueryKsOrderParam {
    @ApiModelProperty(value = "分页游标，首页传 0 或 1；返回里给下一页值")
    private Integer minId = 0;
    @ApiModelProperty(value = "每页条数，≤100")
    private Integer back = 100;
    /** Unix 秒 */
    @ApiModelProperty(value = "开始时间，Unix 秒级时间戳")
    private Long start;
    /** Unix 秒 */
    @ApiModelProperty(value = "结束时间，Unix 秒级时间戳")
    private Long end;
    @ApiModelProperty(value = "时间类型：1付款 2结算 3更新，默认 1")
    private Integer dateType = 1;
    @ApiModelProperty(value = "订单状态：0全部 1已付款 2已收货 3已结算 4已失效")
    private Integer state = 0;
}
