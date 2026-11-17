package com.mailvor.modules.order.service;

import com.mailvor.utils.RedisUtil;

/**
 * 订单落地后的统一副作用入口。
 *
 * <p><b>为什么需要它</b>：「订单落库后刷新该用户今日预估」是一条没说出口的顺序约束，
 * 此前靠 {@code OrderTask} 里 11 处手工调 {@code RedisUtil.setFeeUid} 维持。
 * 穿山甲电商联盟订单服务整体漏了这一步——因为这条规则不在任何 interface 上，
 * 漏了也不会编译失败、不会测试失败，只是用户收益曲线不刷新。</p>
 *
 * <p>本模块把「落库后的副作用」收成一处，并提供唯一的归因串入口，让调用方
 * 拿不到「忘记刷新」和「格式写错」的机会。</p>
 */
public final class OrderLanded {

    private OrderLanded() {
    }

    /**
     * 订单落地后调用：刷新该用户的今日预估队列。
     *
     * @param uid 归因到的用户 uid；null / 非正数表示未归因，不刷新
     */
    public static void landed(Long uid) {
        if (shouldRefreshFee(uid)) {
            RedisUtil.setFeeUid(uid);
        }
    }

    /** uid 有效才刷新预估 */
    public static boolean shouldRefreshFee(Long uid) {
        return uid != null && uid > 0;
    }

    /**
     * 抖音/快手/闲鱼等复用 mw_order_dy 时的归因串。
     * 格式 {@code 773026_<uid>_0200}，第二段是 uid（与 {@code OrderUtil.getDyOrderUser} 兼容）。
     */
    public static final class Attribution {
        private static final String PREFIX = "773026";
        private static final String SUFFIX = "0200";

        private Attribution() {
        }

        public static String douyin(Long uid) {
            return PREFIX + "_" + uid + "_" + SUFFIX;
        }

        public static String douyin(String uid) {
            return PREFIX + "_" + uid + "_" + SUFFIX;
        }

        /** 解析归因串取 uid；格式非法返回 0L */
        public static Long parse(String text) {
            if (text == null || text.isEmpty()) {
                return 0L;
            }
            String[] parts = text.split("_");
            if (parts.length <= 1) {
                return 0L;
            }
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }
}
