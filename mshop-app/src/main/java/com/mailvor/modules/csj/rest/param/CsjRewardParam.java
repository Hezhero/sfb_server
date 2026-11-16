package com.mailvor.modules.csj.rest.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 激励视频奖励上报参数
 */
@Data
@ApiModel(value = "CsjRewardParam", description = "激励视频奖励上报参数")
public class CsjRewardParam {

    @ApiModelProperty(value = "穿山甲奖励流水号（SDK 回调返回，全局唯一）", required = true)
    private String transactionId;

    @ApiModelProperty(value = "广告位ID")
    private String posId;
}
