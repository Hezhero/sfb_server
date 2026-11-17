package com.mailvor.ks;

import com.alibaba.fastjson.JSON;
import com.mailvor.api.ApiResult;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.modules.dataoke.rest.HaodankuController;
import com.mailvor.modules.tk.param.GoodsListKsParam;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.modules.tk.service.KuService;
import com.mailvor.modules.tk.vo.*;
import com.mailvor.modules.user.domain.MwUser;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 快手 REST 端点测试：锁定转链的推客归因（channel=uid）与失败时中文错误提示。
 */
public class KuaishouControllerTest {

    @After
    public void clearUser() {
        LocalUser.clear();
    }

    private HaodankuController controller(KuService service) {
        HaodankuController controller = new HaodankuController();
        ReflectionTestUtils.setField(controller, "kuService", service);
        return controller;
    }

    private void login(long uid) {
        MwUser user = new MwUser();
        user.setUid(uid);
        LocalUser.set(user, 1);
    }

    @Test
    public void ratesUrlUsesLoggedInUidAsChannelForAttribution() {
        KuService service = mock(KuService.class);
        KsItemRatesUrlVo response = JSON.parseObject(
                "{\"code\":1,\"msg\":\"success\",\"data\":{\"linkCode\":\"口令\",\"kwaiUrl\":\"https://kuaishou.com/p\",\"linkUrl\":\"https://s.url\"}}",
                KsItemRatesUrlVo.class);
        when(service.itemRatesUrl("P1", "42")).thenReturn(response);
        login(42L);

        ApiResult<?> result = controller(service).ksItemRatesUrl("P1");

        assertEquals(200, result.getStatus());
        KsItemRatesUrlVo.KsItemRatesUrlData data = (KsItemRatesUrlVo.KsItemRatesUrlData) result.getData();
        assertEquals("口令", data.getLinkCode());
        verify(service).itemRatesUrl("P1", "42");
    }

    @Test
    public void ratesUrlFallsBackToZeroChannelWhenNoUser() {
        KuService service = mock(KuService.class);
        KsItemRatesUrlVo response = JSON.parseObject(
                "{\"code\":1,\"msg\":\"success\",\"data\":{\"linkCode\":\"口令\"}}", KsItemRatesUrlVo.class);
        when(service.itemRatesUrl("P1", "0")).thenReturn(response);

        controller(service).ksItemRatesUrl("P1");

        verify(service).itemRatesUrl("P1", "0");
    }

    @Test
    public void ratesUrlFailureSurfacesChineseErrorNotRawUpstreamCode() {
        KuService service = mock(KuService.class);
        when(service.itemRatesUrl(anyString(), anyString())).thenReturn(null);
        login(42L);
        try {
            controller(service).ksItemRatesUrl("P1");
            fail("应抛出中文错误");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("快手"));
            assertFalse(e.getMessage().contains("code"));
        }
    }

    @Test
    public void itemListDetailAnalyzeAndOrdersPassThrough() {
        KuService service = mock(KuService.class);
        GoodsListKsParam listParam = new GoodsListKsParam();
        listParam.setKeyword("手机");
        KsGoodsListVo listVo = new KsGoodsListVo();
        listVo.setCode(1);
        when(service.itemList(listParam)).thenReturn(listVo);
        KsGoodsDetailVo detailVo = new KsGoodsDetailVo();
        detailVo.setCode(200);
        when(service.itemDetail("P1")).thenReturn(detailVo);
        KsAnalyzeCodeVo analyzeVo = new KsAnalyzeCodeVo();
        analyzeVo.setCode(1);
        KsAnalyzeCodeVo.KsAnalyzeCodeData data = new KsAnalyzeCodeVo.KsAnalyzeCodeData();
        data.setProductId("P9");
        analyzeVo.setData(data);
        when(service.analyzeCode("口令")).thenReturn(analyzeVo);
        QueryKsOrderParam orderParam = new QueryKsOrderParam();
        orderParam.setMinId(0);
        KsOrderResVo orderVo = new KsOrderResVo();
        orderVo.setCode(1);
        when(service.orderList(orderParam)).thenReturn(orderVo);

        HaodankuController controller = controller(service);
        assertEquals(200, controller.ksItemList(listParam).getStatus());
        assertEquals(200, controller.ksItemDetail("P1").getStatus());
        assertEquals("P9", ((KsAnalyzeCodeVo.KsAnalyzeCodeData) controller.ksAnalyzeCode("口令").getData()).getProductId());
        assertEquals(200, controller.ksOrderList(orderParam).getStatus());
    }

    @Test
    public void upstreamFailureIsTranslatedToChineseForEveryReadOnlyEndpoint() {
        KuService service = mock(KuService.class);
        when(service.itemList(any(GoodsListKsParam.class))).thenReturn(null);
        when(service.itemDetail(anyString())).thenReturn(null);
        when(service.analyzeCode(anyString())).thenReturn(null);
        when(service.orderList(any(QueryKsOrderParam.class))).thenReturn(null);
        HaodankuController controller = controller(service);

        assertChineseError(() -> controller.ksItemList(new GoodsListKsParam()));
        assertChineseError(() -> controller.ksItemDetail("P1"));
        assertChineseError(() -> controller.ksAnalyzeCode("口令"));
        assertChineseError(() -> controller.ksOrderList(new QueryKsOrderParam()));
    }

    private void assertChineseError(Runnable action) {
        try {
            action.run();
            fail("应抛出中文错误");
        } catch (IllegalStateException e) {
            assertTrue("错误信息应包含快手且不含原始 code: " + e.getMessage(),
                    e.getMessage().contains("快手") && !e.getMessage().contains("code"));
        }
    }
}
