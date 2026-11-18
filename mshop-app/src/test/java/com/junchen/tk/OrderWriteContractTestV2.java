package com.junchen.tk;

import com.junchen.modules.tk.domain.JunchenTbOrder;
import com.junchen.modules.tk.service.OrderWriteContract;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * 订单表写入契约测试。
 *
 * <p><b>为什么需要这条 seam</b>：{@code mw_order_tb} 有三类写入者（淘宝原生、淘宝更高佣、闲鱼），
 * 但关键不变量只活在某段代码的注释里：
 * <ul>
 *   <li>trade_parent_id 存子单号、parent_id 存父单号（对调）—— 否则同店铺多商品只同步一单</li>
 *   <li>tk_create_time 必须非空 —— 提现与收益曲线都按它过滤</li>
 *   <li>channel_code 纯数字时解析为 uid —— 归因来源</li>
 * </ul>
 * 新 adapter 照着上游字段直译就会漏，且不会报错，只是静默丢数据。</p>
 */
public class OrderWriteContractTestV2 {

    private static final long UNIX_PAID = 1700000000L;

    private static OrderWriteContract.TbSourceRow row() {
        return OrderWriteContract.TbSourceRow.builder()
                .tradeId("10000000000000001")
                .tradeParentId("10000000000000002")
                .itemTitle("商品")
                .itemImg("pic")
                .itemId("ITEM-1")
                .payPrice("99.0")
                .actualCommission(5.0)
                .predictMoney(6.0)
                .paidTime(String.valueOf(UNIX_PAID))
                .commissionRate(5.5)
                .tkStatus(3)
                .channelCode("12345")
                .build();
    }

    @Test
    public void swapsParentAndChildOrderIds() {
        JunchenTbOrder order = OrderWriteContract.toTbOrder(row());

        // 与淘宝原生路径一致的对调：
        //   parent_id ← 上游父单号（10000000000000002）
        //   trade_parent_id ← 上游子单号（10000000000000001）
        assertEquals(Long.valueOf(10000000000000002L), order.getParentId());
        assertEquals(Long.valueOf(10000000000000001L), order.getTradeParentId());
        assertEquals("10000000000000001", order.getTradeId());
    }

    @Test
    public void createTimeNeverNull() {
        JunchenTbOrder order = OrderWriteContract.toTbOrder(row());
        assertNotNull("tk_create_time 必须非空，否则提现会静默跳过", order.getTkCreateTime());
        assertEquals(UNIX_PAID * 1000L, order.getTkCreateTime().getTime());
    }

    @Test
    public void createTimeFallsBackToNowWhenPaidTimeMissing() {
        OrderWriteContract.TbSourceRow r = row();
        r.setPaidTime(null);
        long before = System.currentTimeMillis();
        JunchenTbOrder order = OrderWriteContract.toTbOrder(r);
        assertNotNull(order.getTkCreateTime());
        org.junit.Assert.assertTrue(order.getTkCreateTime().getTime() >= before - 1000L);
    }

    @Test
    public void parsesUidFromNumericChannelCode() {
        JunchenTbOrder order = OrderWriteContract.toTbOrder(row());
        assertEquals(Long.valueOf(12345L), order.getUid());
    }

    @Test
    public void nonNumericChannelCodeYieldsNullUid() {
        OrderWriteContract.TbSourceRow r = row();
        r.setChannelCode("not-numeric");
        JunchenTbOrder order = OrderWriteContract.toTbOrder(r);
        assertNull(order.getUid());
    }

    @Test
    public void blankChannelCodeYieldsNullUid() {
        OrderWriteContract.TbSourceRow r = row();
        r.setChannelCode(null);
        JunchenTbOrder order = OrderWriteContract.toTbOrder(r);
        assertNull(order.getUid());
    }

    @Test
    public void actualCommissionPreferredOverPredicted() {
        JunchenTbOrder order = OrderWriteContract.toTbOrder(row());
        assertEquals("实际佣金优先", Double.valueOf(5.0), order.getPubSharePreFee());
    }

    @Test
    public void predictedCommissionUsedWhenActualMissing() {
        OrderWriteContract.TbSourceRow r = row();
        r.setActualCommission(null);
        JunchenTbOrder order = OrderWriteContract.toTbOrder(r);
        assertEquals("实际佣金缺失时用预估佣金", Double.valueOf(6.0), order.getPubSharePreFee());
    }
}
