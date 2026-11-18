package com.junchen.order;

import com.junchen.modules.order.service.OrderLanded;
import com.junchen.utils.OrderUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 归因串与订单落地副作用测试。
 *
 * <p><b>为什么需要</b>：{@code 773026_<uid>_0200} 这个格式在生产者（拼串）和消费者
 * （{@code OrderUtil.getDyOrderUser} 切下划线取第二段）各写了一遍。而「订单落库后
 * 刷新今日预估」靠 11 处手工调 {@code RedisUtil.setFeeUid} 维持——穿山甲订单服务
 * 就整体漏了这一步，且没有任何测试能发现，因为它不在任何 interface 上。</p>
 */
public class OrderLandedTest {

    @Test
    public void attributionRoundTripsWithLegacyFormat() {
        String text = OrderLanded.Attribution.douyin(12345L);
        assertEquals("773026_12345_0200", text);
        assertEquals(Long.valueOf(12345L), OrderLanded.Attribution.parse(text));
        // 与既有 OrderUtil 解析器保持兼容（不能改变线上数据格式）
        assertEquals(Long.valueOf(12345L), OrderUtil.getDyOrderUser(text));
    }

    @Test
    public void attributionRejectsMalformedText() {
        assertEquals(Long.valueOf(0L), OrderLanded.Attribution.parse(null));
        assertEquals(Long.valueOf(0L), OrderLanded.Attribution.parse(""));
        assertEquals(Long.valueOf(0L), OrderLanded.Attribution.parse("garbage"));
        assertEquals(Long.valueOf(0L), OrderLanded.Attribution.parse("773026_abc_0200"));
    }

    @Test
    public void nonPositiveUidIsNotQueuedForFeeRefresh() {
        // uid<=0 不应刷新预估（与现有调用点的判断一致）
        assertFalse(OrderLanded.shouldRefreshFee(null));
        assertFalse(OrderLanded.shouldRefreshFee(0L));
        assertFalse(OrderLanded.shouldRefreshFee(-1L));
        assertTrue(OrderLanded.shouldRefreshFee(123L));
    }
}
