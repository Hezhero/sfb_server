package com.mailvor.tk;

import com.mailvor.modules.tk.domain.MailvorIdleOrder;
import com.mailvor.modules.tk.domain.MailvorTbLiveOrder;
import com.mailvor.modules.tk.domain.MailvorTbOrder;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * 订单写入契约回归测试：新平台订单必须带 tk_create_time。
 *
 * <p><b>为什么必须有这条测试</b>：提现相关 SQL
 * （{@code MailvorTbOrderMapper.totalCash / selectCashOrderId / updateCash}）
 * 与收益曲线（{@code chartList}）全部按 {@code tk_create_time} 过滤。
 * 该列为 NULL 时订单不会报错，只是<strong>静默地不属于任何人的可提现余额</strong>，
 * 也不出现在收益曲线里。</p>
 *
 * <p>2026-11-17 的架构审查发现了这个问题：闲鱼与淘宝更高佣的 {@code convert()}
 * 只设了 {@code tkPaidTime}，漏了 {@code tkCreateTime}。</p>
 */
public class OrderWriteContractTest {

    private static final long UNIX_PAID = 1700000000L;

    @Test
    public void tbLiveOrderSetsCreateTimeFromPaidTime() {
        MailvorTbLiveOrder row = MailvorTbLiveOrder.builder()
                .tradeId("T1")
                .tradeParentId("P1")
                .payPrice(99.0)
                .predictMoney(5.0)
                .paidTime(String.valueOf(UNIX_PAID))
                .build();

        MailvorTbOrder order = row.convert();
        assertNotNull("淘宝更高佣订单必须带 tkCreateTime，否则提现会静默跳过", order.getTkCreateTime());
        assertEquals(UNIX_PAID * 1000L, order.getTkCreateTime().getTime());
    }

    @Test
    public void tbLiveOrderFallsBackToNowWhenPaidTimeMissing() {
        MailvorTbLiveOrder row = MailvorTbLiveOrder.builder()
                .tradeId("T2")
                .payPrice(50.0)
                .predictMoney(2.0)
                .build();

        long before = System.currentTimeMillis();
        MailvorTbOrder order = row.convert();
        assertNotNull("付款时间缺失时也必须兜底，宁时间略偏也不能丢单", order.getTkCreateTime());
        org.junit.Assert.assertTrue(order.getTkCreateTime().getTime() >= before - 1000L);
    }

    @Test
    public void idleOrderSetsCreateTimeFromPaidTime() {
        MailvorIdleOrder row = MailvorIdleOrder.builder()
                .tradeId("I1")
                .tradeParentId("IP1")
                .payPrice(88.0)
                .predictMoney(4.0)
                .paidTime(String.valueOf(UNIX_PAID))
                .build();

        MailvorTbOrder order = row.convert();
        assertNotNull("闲鱼订单必须带 tkCreateTime，否则提现会静默跳过", order.getTkCreateTime());
        assertEquals(UNIX_PAID * 1000L, order.getTkCreateTime().getTime());
    }

    @Test
    public void idleOrderFallsBackToNowWhenPaidTimeMissing() {
        MailvorIdleOrder row = MailvorIdleOrder.builder()
                .tradeId("I2")
                .payPrice(20.0)
                .predictMoney(1.0)
                .build();

        MailvorTbOrder order = row.convert();
        assertNotNull("付款时间缺失时也必须兜底", order.getTkCreateTime());
    }
}
