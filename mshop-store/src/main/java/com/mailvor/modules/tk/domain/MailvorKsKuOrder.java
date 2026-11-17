package com.mailvor.modules.tk.domain;

import com.alibaba.fastjson.annotation.JSONField;
import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 好单库快手订单原始行（ks_order_list）。
 *
 * <p>按官方规格字段命名，再 convert 成 {@link MailvorDyOrder} 复用 mw_order_dy 表、
 * 分佣与后台页面——与 DyKuOrder / EleKuOrder 的落地方式一致，不新增 TkOrder 子类，
 * 因而无需改动 7 张平台订单表的 DDL。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailvorKsKuOrder {
    /** 子订单号 */
    @JSONField(name = "trade_id")
    private String tradeId;
    /** 父订单号 */
    @JSONField(name = "trade_parent_id")
    private String tradeParentId;
    /** 订单金额 */
    @JSONField(name = "pay_price")
    private Double payPrice;
    /** 预估佣金 */
    @JSONField(name = "predict_money")
    private Double predictMoney;
    /** 实际佣金 */
    @JSONField(name = "actual_money")
    private Double actualMoney;
    /** 结算状态：0待结算 1已结算 2不予结算 */
    @JSONField(name = "settled_status")
    private Integer settledStatus;
    /** 订单状态：1已付款 2已收货 3已结算 4已失效 */
    @JSONField(name = "order_status")
    private Integer orderStatus;
    /** 渠道标识（转链时的 channel，即推客 uid） */
    @JSONField(name = "channel_code")
    private String channelCode;
    /** 付款时间，Unix 秒 */
    @JSONField(name = "paid_time")
    private Long paidTime;
    /** 结算时间，Unix 秒 */
    @JSONField(name = "earning_time")
    private Long earningTime;

    public MailvorDyOrder convert() {
        long paid = paidTime == null ? 0L : paidTime * 1000L;
        long earn = earningTime == null ? 0L : earningTime * 1000L;
        return MailvorDyOrder.builder()
                .orderId(tradeId)
                .totalPayAmount(payPrice)
                .estimatedTotalCommission(predictMoney)
                .realCommission(actualMoney)
                .paySuccessTime(paid > 0 ? new Date(paid) : null)
                .settleTime(earn > 0 ? new Date(earn) : null)
                .flowPoint(flowPoint(orderStatus, settledStatus))
                // 复用现有 external_info 归因机制（773026_<uid>_0200 中间段为 uid）
                .externalInfo("773026_" + channelCode + "_0200")
                .commissionRate(commissionRate())
                .innerType(0)
                .build();
    }

    /** 已失效订单标记为 REFUND，走现有无效订单过滤逻辑；其余按订单状态映射。 */
    private String flowPoint(Integer orderStatus, Integer settledStatus) {
        if (settledStatus != null && settledStatus == 2) {
            return "REFUND";
        }
        if (orderStatus == null) {
            return "PAY_SUCC";
        }
        switch (orderStatus) {
            case 2:
                return "CONFIRM";
            case 3:
                return "SETTLE";
            case 4:
                return "REFUND";
            default:
                return "PAY_SUCC";
        }
    }

    /** 佣金比率 = 预估佣金 / 订单金额，按百分比保留两位，避免除零。 */
    private Double commissionRate() {
        if (predictMoney == null || payPrice == null || payPrice <= 0) {
            return 0D;
        }
        return NumberUtil.round(NumberUtil.mul(NumberUtil.div(predictMoney, payPrice), 100D), 2).doubleValue();
    }
}
