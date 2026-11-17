package com.mailvor.modules.tk.domain;

import com.alibaba.fastjson.annotation.JSONField;
import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好单库闲鱼订单原始行（idle_order_list）。
 *
 * <p>convert 成 {@link MailvorTbOrder} 复用 mw_order_tb 表与现有分佣/冲正链路，
 * 不新增 TkOrder 子类，因而无需改动 7 张订单表的 DDL。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailvorIdleOrder {
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
    /** 1创建 2交易完成 3交易取消 4交易已付款 */
    @JSONField(name = "order_status")
    private Integer orderStatus;
    /** 0待结算 1已结算 2不予结算 */
    @JSONField(name = "settled_status")
    private Integer settledStatus;
    @JSONField(name = "channel_code")
    private String channelCode;

    public MailvorTbOrder convert() {
        MailvorTbOrder order = MailvorTbOrder.builder()
                .tradeParentId(parseLong(tradeParentId))
                .tradeId(tradeId)
                .itemTitle(itemTitle)
                .itemImg(itemImg)
                .itemId(itemId)
                .payPrice(payPrice == null ? null : String.valueOf(payPrice))
                .pubSharePreFee(actualMoney != null ? actualMoney : predictMoney)
                .tkPaidTime(parseDate(paidTime))
                .tkTotalRate(commissionRate())
                .build();
        if (channelCode != null && channelCode.chars().allMatch(Character::isDigit)) {
            try {
                order.setUid(Long.valueOf(channelCode));
            } catch (NumberFormatException ignored) {
            }
        }
        return order;
    }

    private Double commissionRate() {
        if (predictMoney == null || payPrice == null || payPrice <= 0) {
            return 0D;
        }
        return NumberUtil.round(NumberUtil.mul(NumberUtil.div(predictMoney, payPrice), 100D), 2).doubleValue();
    }

    private static Long parseLong(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static java.util.Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return new java.util.Date(Long.parseLong(s.trim()) * 1000L); }
        catch (NumberFormatException e) { return null; }
    }
}
