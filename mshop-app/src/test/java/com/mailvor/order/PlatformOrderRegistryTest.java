package com.mailvor.order;

import com.mailvor.modules.order.service.PlatformOrderRegistry;
import com.mailvor.modules.tk.domain.MailvorTbOrder;
import com.mailvor.modules.tk.domain.TkOrder;
import com.mailvor.modules.tk.service.MailvorTbOrderService;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 平台订单注册表测试。
 *
 * <p><b>为什么需要</b>：「哪些平台参与退款/风控/计费」此前在 7 个文件里逐个字段枚举，
 * 每个文件漏的平台还不一样（Ele 全漏、Mt 部分漏），漏接是静默的——不报错、
 * 不测试失败，只是那部分订单永远不参与 refund/计费。
 * 注册表把这些 adapter 收成一处，让漏接变成可发现的。</p>
 */
public class PlatformOrderRegistryTest {

    @Test
    public void sourceDelegatesFindToItsPlatformService() {
        MailvorTbOrderService service = mock(MailvorTbOrderService.class);
        MailvorTbOrder order = new MailvorTbOrder();
        when(service.getRefundAndBindOrderList(30)).thenReturn(List.of(order));

        PlatformOrderRegistry.OrderSource source =
                PlatformOrderRegistry.OrderSource.tb("淘宝", service);

        assertEquals("淘宝", source.platform());
        assertEquals(1, source.findRefundable(30).size());
        verify(service).getRefundAndBindOrderList(30);
    }

    @Test
    public void allSourcesShareSameInterface() {
        // 注册表持有的是 List<OrderSource>，新增平台 = 新增一个 adapter
        // 聚合点只遍历列表，不再逐字段注入 —— 漏接可以从注册表清单一眼看出
        MailvorTbOrderService service = mock(MailvorTbOrderService.class);
        List<PlatformOrderRegistry.OrderSource> sources =
                List.of(PlatformOrderRegistry.OrderSource.tb("淘宝", service));
        assertEquals(1, sources.size());
        assertTrue(sources.get(0) instanceof PlatformOrderRegistry.OrderSource);
    }

    @Test
    public void sourceCarriesDomainTypeForDispatch() {
        MailvorTbOrderService service = mock(MailvorTbOrderService.class);
        PlatformOrderRegistry.OrderSource source =
                PlatformOrderRegistry.OrderSource.tb("淘宝", service);
        assertEquals(MailvorTbOrder.class, source.domainType());
    }
}
