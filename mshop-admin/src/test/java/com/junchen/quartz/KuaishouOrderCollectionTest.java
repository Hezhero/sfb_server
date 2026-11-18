package com.junchen.quartz;

import com.alibaba.fastjson.JSON;
import com.junchen.modules.quartz.task.OrderTask;
import com.junchen.modules.tk.domain.JunchenDyOrder;
import com.junchen.modules.tk.domain.JunchenKsKuOrder;
import com.junchen.modules.tk.param.QueryKsOrderParam;
import com.junchen.modules.tk.service.KuService;
import com.junchen.modules.tk.service.JunchenDyOrderService;
import com.junchen.modules.tk.vo.KsOrderResVo;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 快手订单落库测试：Unix 秒转 Date、channel_code 归因 uid、返回 min_id 游标。
 * 订单复用 mw_order_dy（与 DyKu/EleKu 同一模式），不新增 TkOrder 子类，故无需动 7 张表 DDL。
 */
public class KuaishouOrderCollectionTest {

    @Test
    public void convertMapsFieldsSecondsAndAttribution() {
        KsOrderResVo res = JSON.parseObject(
                "{\"code\":1,\"msg\":\"success\",\"min_id\":55,\"data\":[{"
                        + "\"trade_id\":\"T1\",\"trade_parent_id\":\"TP1\",\"pay_price\":29.9,"
                        + "\"predict_money\":3.5,\"actual_money\":3.0,\"settled_status\":1,"
                        + "\"order_status\":1,\"channel_code\":\"uid42\","
                        + "\"paid_time\":1700000100,\"earning_time\":1700000200}]}",
                KsOrderResVo.class);
        JunchenKsKuOrder ku = res.getData().get(0);
        JunchenDyOrder order = ku.convert();

        assertEquals("T1", order.getOrderId());
        assertEquals(Double.valueOf(29.9D), order.getTotalPayAmount());
        assertEquals(Double.valueOf(3.5D), order.getEstimatedTotalCommission());
        assertEquals(Double.valueOf(3.0D), order.getRealCommission());
        assertEquals(new Date(1700000100L * 1000), order.getPaySuccessTime());
        assertEquals(new Date(1700000200L * 1000), order.getSettleTime());
        assertEquals("PAY_SUCC", order.getFlowPoint());
        assertEquals("773026_uid42_0200", order.getExternalInfo());
    }

    @Test
    public void saveKsReturnsCursorAndBindsUidFromChannelCode() {
        KuService kuService = mock(KuService.class);
        JunchenDyOrderService dyOrderService = mock(JunchenDyOrderService.class);
        KsOrderResVo res = JSON.parseObject(
                "{\"code\":1,\"msg\":\"success\",\"min_id\":55,\"data\":[{"
                        + "\"trade_id\":\"T2\",\"trade_parent_id\":\"TP2\",\"pay_price\":9.9,"
                        + "\"predict_money\":1.5,\"actual_money\":1.0,\"settled_status\":0,"
                        + "\"order_status\":1,\"channel_code\":\"88\","
                        + "\"paid_time\":1700000100,\"earning_time\":0}]}",
                KsOrderResVo.class);
        when(kuService.orderList(any(QueryKsOrderParam.class))).thenReturn(res);

        OrderTask task = new OrderTask();
        ReflectionTestUtils.setField(task, "kuService", kuService);
        ReflectionTestUtils.setField(task, "dyOrderService", dyOrderService);

        QueryKsOrderParam param = new QueryKsOrderParam();
        param.setMinId(0);
        param.setBack(100);
        param.setStart(1700000000L);
        param.setEnd(1700100000L);

        Integer next = task.saveKs(param);

        assertEquals(Integer.valueOf(55), next);
        org.mockito.ArgumentCaptor<java.util.Collection<JunchenDyOrder>> captor =
                org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
        verify(dyOrderService).saveOrUpdateBatch(captor.capture());
        java.util.Collection<JunchenDyOrder> saved = captor.getValue();
        assertEquals(1, saved.size());
        JunchenDyOrder o = saved.iterator().next();
        assertEquals(Long.valueOf(88L), o.getUid());
        assertEquals("T2", o.getOrderId());
    }

    @Test
    public void saveKsReturnsZeroOnFailureOrEmpty() {
        KuService kuService = mock(KuService.class);
        JunchenDyOrderService dyOrderService = mock(JunchenDyOrderService.class);
        when(kuService.orderList(any(QueryKsOrderParam.class))).thenReturn(null);

        OrderTask task = new OrderTask();
        ReflectionTestUtils.setField(task, "kuService", kuService);
        ReflectionTestUtils.setField(task, "dyOrderService", dyOrderService);

        QueryKsOrderParam param = new QueryKsOrderParam();
        assertEquals(Integer.valueOf(0), task.saveKs(param));
        verifyNoInteractions(dyOrderService);
    }
}
