package com.mailvor.modules.tk.domain;

import com.alibaba.fastjson.annotation.JSONField;
import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 好单库「淘宝更高佣订单」原始行（tb_live_order_list）。
 *
 * <p>convert 成 {@link MailvorTbOrder} 复用 mw_order_tb 表与现有分佣/退款冲正逻辑，
 * 不新增 TkOrder 子类，因而无需改动 7 张订单表的 DDL。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailvorTbLiveOrder {
    @JSONField(name = "trade_id")
    private String tradeId;
    @JSONField(name = "trade_parent_id")
    private String tradeParentId;
    @JSONField(name = "item_title")
    private String itemTitle;
    @JSONField(name = "item_img")
    private String itemImg;
    @JSONField(name = "item_id")
    private String itemId;
    @JSONField(name = "pay_price")
    private Double payPrice;
    @JSONField(name = "predict_money")
    private Double predictMoney;
    @JSONField(name = "actual_money")
    private Double actualMoney;
    @JSONField(name = "paid_time")
    private String paidTime;
    @JSONField(name = "earning_time")
    private String earningTime;
    @JSONField(name = "order_status")
    private Integer orderStatus;
    @JSONField(name = "settled_status")
    private Integer settledStatus;
    @JSONField(name = "channel_code")
    private String channelCode;

    /** 转成淘宝订单，复用现有落库/分佣/冲正链路 */
    public MailvorTbOrder convert() {
        Date created = parseDate(paidTime);
        // tk_create_time 必须非空：提现（totalCash/selectCashOrderId/updateCash）与收益曲线
        // （chartList）全部按该列过滤，为空会让订单静默消失在提现之外。
        // 上游未给付款时间时用当前时间兜底——宁可时间略偏，也不能丢单。
        if (created == null) {
            created = new Date();
        }
        MailvorTbOrder order = MailvorTbOrder.builder()
                .tradeParentId(parseLong(tradeParentId))
                .tradeId(tradeId)
                .itemTitle(itemTitle)
                .itemImg(itemImg)
                .itemId(itemId)
                .payPrice(payPrice == null ? null : String.valueOf(payPrice))
                .pubSharePreFee(actualMoney != null ? actualMoney : predictMoney)
                .tkPaidTime(created)
                .tkCreateTime(created)
                .tkTotalRate(commissionRate())
                .tkStatus(orderStatus)
                .build();
        // 高佣转链时 channel 传的是推客 uid，订单回传的 channel_code 即 uid。
        // 与抖音 external_info 归因不同，这里直接解析 channel_code。
        if (channelCode != null && channelCode.chars().allMatch(Character::isDigit)) {
            try {
                order.setUid(Long.valueOf(channelCode));
            } catch (NumberFormatException ignored) {
                // channel_code 非纯数字时不归因，落库后由人工/后续任务处理
            }
        }
        return order;
    }

    private static Long parseLong(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double commissionRate() {
        if (predictMoney == null || payPrice == null || payPrice <= 0) {
            return 0D;
        }
        return NumberUtil.round(NumberUtil.mul(NumberUtil.div(predictMoney, payPrice), 100D), 2).doubleValue();
    }

    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new Date(Long.parseLong(s.trim()) * 1000L);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
