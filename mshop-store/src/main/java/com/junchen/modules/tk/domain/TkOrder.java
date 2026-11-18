/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junchen.domain.BaseDomain;
import lombok.*;

import java.util.Date;

/**
* @author shenji
* @date 2022-08-29
*/
@Data
public class TkOrder extends BaseDomain {

    /** 是否拆过红包，0=未拆红包 1=已拆红包 2=已退款 3=已失效*/
    private Integer bind;

    /** 用户ID */
    private Long uid;
    /** 拆红包奖励金额 */
    private Double hb;

    /** 内部类型 0=默认*/
    private Integer innerType;

    /** 拆红包时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date spreadHbTime;

    // ----- 违规订单标记（工单 04）-----
    // 淘宝联盟判定违规（店铺淘宝客 / 订单虚假交易）后冻结或扣佣，这些订单不能再参与结算。
    // 只用独立的 foul_* 三列承载，绝不复用 bind / tk_status / refund_tag：
    // 那三个字段是退款冲正（OrderRefundTask → decHbAndRefundOrder）的选单依据，
    // 一旦被违规逻辑改写，冲正会漏单或被误触发，两种资损叠加。

    /** 违规状态：null/0=正常可结算 1=违规冻结不可结算（官方 punish_status，1=解冻时置回 0） */
    private Integer foulStatus;

    /** 违规类型：1=店铺淘宝客 2=订单虚假交易（官方 violation_type）；无法识别时为 null */
    private Integer violationType;

    /** 违规发现时间，即首次被判冻结的时间；解冻时清空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date foulTime;

}
