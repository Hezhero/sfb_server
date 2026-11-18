package com.junchen.modules.tk.service;

import com.junchen.modules.tk.domain.JunchenTbOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * {@code mw_order_tb} 的写入契约：所有写这张表的路径都必须经过这里。
 *
 * <p><b>为什么需要它</b>：这张表有三个写入者（淘宝原生、淘宝更高佣、闲鱼），
 * 但关键不变量此前只活在某段代码的注释里，新 adapter 照上游字段直译就会漏，
 * 且不报错——只是静默丢数据（uid 绑定不到、退款冲正匹配不到、提现查不到）。</p>
 *
 * <p><b>统一的不变量</b>：</p>
 * <ol>
 *   <li><b>父子单对调</b>：{@code parent_id} ← 上游父单号，{@code trade_parent_id} ← 上游子单号。
 *       原因：同店铺多商品父单号相同，若 trade_parent_id 存父单号则只会同步一单；
 *       且 uid 绑定/退款冲正都按 trade_parent_id 关联，必须语义唯一。</li>
 *   <li><b>tk_create_time 非空</b>：提现（totalCash/selectCashOrderId/updateCash）与
 *       收益曲线（chartList）全部按该列过滤；上游未给时间时用当前时间兜底。</li>
 *   <li><b>归因字段</b>：{@code channel_code} 为纯数字时即推客 uid。</li>
 * </ol>
 *
 * <p>本模块只负责「上游原始行 → 符合契约的订单行」这一段，不做 HTTP、不落库、
 * 不刷新今日预估——那些是各自 adapter / 调用方的事。</p>
 */
public final class OrderWriteContract {

    private OrderWriteContract() {
    }

    /**
     * 上游订单行的通用形状。各平台 adapter 只需把自己的响应字段填进来，
     * 不用各自实现一遍对调/兜底/归因。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TbSourceRow {
        /** 子订单号（上游 trade_id） */
        private String tradeId;
        /** 父订单号（上游 trade_parent_id） */
        private String tradeParentId;
        private String itemTitle;
        private String itemImg;
        private String itemId;
        private String payPrice;
        /** 实际佣金，优先使用 */
        private Double actualCommission;
        /** 预估佣金，actualCommission 缺失时用 */
        private Double predictMoney;
        /** 付款时间（Unix 秒字符串） */
        private String paidTime;
        private Double commissionRate;
        /** 官方订单状态 */
        private Integer tkStatus;
        /** 转链时传入的 channel，纯数字时为推客 uid */
        private String channelCode;
    }

    /**
     * 把上游行转成符合 {@code mw_order_tb} 写入契约的订单。
     */
    public static JunchenTbOrder toTbOrder(TbSourceRow row) {
        if (row == null) {
            throw new IllegalArgumentException("上游订单行不能为空");
        }

        Date created = parseUnixSeconds(row.getPaidTime());
        if (created == null) {
            created = new Date();
        }

        Double commission = row.getActualCommission() != null
                ? row.getActualCommission() : row.getPredictMoney();

        JunchenTbOrder order = JunchenTbOrder.builder()
                // 对调：parent_id ← 父单，trade_parent_id ← 子单
                .parentId(parseLong(row.getTradeParentId()))
                .tradeParentId(parseLong(row.getTradeId()))
                .tradeId(row.getTradeId())
                .itemTitle(row.getItemTitle())
                .itemImg(row.getItemImg())
                .itemId(row.getItemId())
                .payPrice(row.getPayPrice())
                .pubSharePreFee(commission)
                .tkPaidTime(created)
                .tkCreateTime(created)
                .tkTotalRate(row.getCommissionRate())
                .tkStatus(row.getTkStatus())
                .build();

        // channel_code 纯数字 → 推客 uid
        String channel = row.getChannelCode();
        if (channel != null && channel.chars().allMatch(Character::isDigit)) {
            try {
                order.setUid(Long.valueOf(channel));
            } catch (NumberFormatException ignored) {
                // 超长数字串无法转 Long，不归因
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

    private static Date parseUnixSeconds(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            long sec = Long.parseLong(s.trim());
            return sec <= 0 ? null : new Date(sec * 1000L);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
