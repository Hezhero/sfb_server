package com.mailvor.quartz;

import com.mailvor.modules.quartz.task.KsOrderCollectionTask;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.modules.tk.service.KuService;
import com.mailvor.modules.tk.service.MailvorDyOrderService;
import com.mailvor.modules.tk.vo.KsOrderResVo;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 快手增量采集任务测试：Unix 秒时间窗默认值与 min_id 游标翻页。
 */
public class KsOrderCollectionTaskTest {

    @Test
    public void runDefaultsToUnixSecondsWindow() {
        KuService kuService = mock(KuService.class);
        MailvorDyOrderService dyOrderService = mock(MailvorDyOrderService.class);
        when(kuService.orderList(any(QueryKsOrderParam.class))).thenReturn(new KsOrderResVo());
        KsOrderCollectionTask task = new KsOrderCollectionTask();
        ReflectionTestUtils.setField(task, "kuService", kuService);
        ReflectionTestUtils.setField(task, "dyOrderService", dyOrderService);

        task.run(null);

        org.mockito.ArgumentCaptor<QueryKsOrderParam> captor =
                org.mockito.ArgumentCaptor.forClass(QueryKsOrderParam.class);
        verify(kuService).orderList(captor.capture());
        QueryKsOrderParam param = captor.getValue();
        assertNotNull(param.getStart());
        assertNotNull(param.getEnd());
        assertTrue("默认窗口为 10 分钟（Unix 秒）", param.getEnd() - param.getStart() == 600L);
    }

    private java.util.ArrayList<com.mailvor.modules.tk.domain.MailvorKsKuOrder> orderRows() {
        java.util.ArrayList<com.mailvor.modules.tk.domain.MailvorKsKuOrder> rows = new java.util.ArrayList<>();
        com.mailvor.modules.tk.domain.MailvorKsKuOrder row = new com.mailvor.modules.tk.domain.MailvorKsKuOrder();
        row.setTradeId("T");
        row.setChannelCode("88");
        row.setPaidTime(1700000000L);
        row.setEarningTime(0L);
        row.setOrderStatus(1);
        row.setPayPrice(9.9D);
        rows.add(row);
        return rows;
    }

    @Test
    public void cursorContinuesPagingWhenReturnedMinIdAdvances() {
        KuService kuService = mock(KuService.class);
        MailvorDyOrderService dyOrderService = mock(MailvorDyOrderService.class);
        KsOrderResVo p1 = new KsOrderResVo();
        p1.setCode(1);
        p1.setMinId(5);
        p1.setData(orderRows());
        KsOrderResVo p2 = new KsOrderResVo();
        p2.setCode(1);
        p2.setMinId(5);
        p2.setData(orderRows());
        when(kuService.orderList(any(QueryKsOrderParam.class))).thenReturn(p1, p2);
        KsOrderCollectionTask task = new KsOrderCollectionTask();
        ReflectionTestUtils.setField(task, "kuService", kuService);
        ReflectionTestUtils.setField(task, "dyOrderService", dyOrderService);

        QueryKsOrderParam param = new QueryKsOrderParam();
        param.setStart(1000L);
        param.setEnd(2000L);
        task.saveKsOrders(param);

        verify(kuService, times(2)).orderList(any(QueryKsOrderParam.class));
        assertEquals(Integer.valueOf(5), param.getMinId());
    }
}
