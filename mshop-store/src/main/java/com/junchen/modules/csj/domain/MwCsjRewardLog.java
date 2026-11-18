package com.junchen.modules.csj.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.junchen.domain.BaseDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 穿山甲激励视频奖励记录
 *
 * <p>transaction_id 建唯一索引，用于防重放：同一个奖励流水号只能发一次积分。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mw_csj_reward_log")
@ApiModel(value = "MwCsjRewardLog对象", description = "穿山甲激励视频奖励记录")
public class MwCsjRewardLog extends BaseDomain {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户uid")
    private Long uid;

    @ApiModelProperty(value = "穿山甲奖励流水号，全局唯一")
    private String transactionId;

    @ApiModelProperty(value = "广告位ID")
    private String posId;

    @ApiModelProperty(value = "发放积分")
    private Integer integral;

    @ApiModelProperty(value = "状态：1已发放 2已拒绝")
    private Integer status;

    @ApiModelProperty(value = "备注/拒绝原因")
    private String remark;

    @ApiModelProperty(value = "请求IP")
    private String ip;
}
