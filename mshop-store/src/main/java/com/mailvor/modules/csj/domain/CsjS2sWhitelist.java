package com.mailvor.modules.csj.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mailvor.domain.BaseDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 穿山甲 S2S 回调奖励白名单。
 *
 * <p>S2S 验签通过后落库一条「可发放」记录；客户端上报奖励时比对，
 * 防止被 hook 的客户端仅凭自己生成的 transactionId 就领到积分。</p>
 *
 * @see com.mailvor.modules.csj.service.CsjS2sSigner
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mw_csj_s2s_whitelist")
@ApiModel(value = "CsjS2sWhitelist对象", description = "穿山甲S2S奖励白名单")
public class CsjS2sWhitelist extends BaseDomain {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "穿山甲回调 trans_id，全局唯一")
    private String transId;

    @ApiModelProperty(value = "回调回传 user_id")
    private Long uid;

    @ApiModelProperty(value = "奖励名称")
    private String rewardName;

    @ApiModelProperty(value = "奖励数量")
    private Integer rewardAmount;

    @ApiModelProperty(value = "回调 extra 原样透传")
    private String extra;

    /** 是否已消费：0 可发放，1 已发放 */
    public static final int CONSUMED_NO = 0;
    public static final int CONSUMED_YES = 1;

    @ApiModelProperty(value = "是否已消费：0可发放 1已发放")
    private Integer consumed;

    @ApiModelProperty(value = "回调来源 IP")
    private String sourceIp;
}
