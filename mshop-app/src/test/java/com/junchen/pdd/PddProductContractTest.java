package com.junchen.pdd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.dataoke.rest.DataokePDDController;
import com.junchen.modules.tk.config.PddConfig;
import com.junchen.modules.tk.param.GoodsListPddParam;
import com.junchen.modules.tk.service.DataokeService;
import com.junchen.modules.tk.service.PddService;
import com.junchen.modules.tk.vo.pdd.*;
import com.junchen.utils.RedisUtils;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.api.pop.request.*;
import com.pdd.pop.sdk.http.api.pop.response.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PddProductContractTest {
    private final PopClient client = mock(PopClient.class);
    private final PddConfig config = new PddConfig();

    private PddService sdk() {
        config.setPid("pid-test");
        // 生产中 paramPrefix 由 Spring 从配置注入；这里显式给定，避免依赖未注入的 null 值
        config.setParamPrefix("sfbuid");
        PddService service = new PddService();
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.setField(service, "pddConfig", config);
        return service;
    }

    @Test
    public void officialDetailConvertsCentsAndPermilleWithoutSubtractingCouponTwice() throws Exception {
        PddDdkGoodsDetailResponse response = mock(PddDdkGoodsDetailResponse.class, RETURNS_DEEP_STUBS);
        PddDdkGoodsDetailResponse.GoodsDetailResponseGoodsDetailsItem item =
                mock(PddDdkGoodsDetailResponse.GoodsDetailResponseGoodsDetailsItem.class);
        when(item.getGoodsSign()).thenReturn("signed-product");
        when(item.getGoodsName()).thenReturn("商品名称");
        when(item.getMinNormalPrice()).thenReturn(5990L);
        when(item.getMinGroupPrice()).thenReturn(3990L);
        when(item.getPromotionRate()).thenReturn(200L);
        when(item.getCouponDiscount()).thenReturn(1000L);
        when(item.getGoodsGalleryUrls()).thenReturn(Collections.singletonList("image"));
        when(response.getGoodsDetailResponse().getGoodsDetails()).thenReturn(Collections.singletonList(item));
        when(client.syncInvoke(any(PddDdkGoodsDetailRequest.class))).thenReturn(response);
        PddGoodsDetailVO result = sdk().getGoodsDetail("signed-product");
        assertEquals(39.9, result.getEndPrice(), 0.001);
        assertEquals(59.9, result.getStartPrice(), 0.001);
        assertEquals(10, result.getCoupon(), 0.001);
        assertEquals(20, result.getFeeRatio(), 0.001);
        assertEquals(7.98, result.getFee(), 0.001);
        assertEquals("signed-product", result.getGoodsId());
        assertEquals(result.getBanners(), result.getDetails());
        ArgumentCaptor<PddDdkGoodsDetailRequest> request = ArgumentCaptor.forClass(PddDdkGoodsDetailRequest.class);
        verify(client).syncInvoke(request.capture());
        assertEquals("signed-product", request.getValue().getParamsMap().get("goods_sign"));
        assertEquals("pid-test", request.getValue().getParamsMap().get("pid"));
    }

    private PddDdkGoodsPromotionUrlGenerateResponse promotion() {
        PddDdkGoodsPromotionUrlGenerateResponse response = mock(PddDdkGoodsPromotionUrlGenerateResponse.class, RETURNS_DEEP_STUBS);
        PddDdkGoodsPromotionUrlGenerateResponse.GoodsPromotionUrlGenerateResponseGoodsPromotionUrlListItem item =
                mock(PddDdkGoodsPromotionUrlGenerateResponse.GoodsPromotionUrlGenerateResponseGoodsPromotionUrlListItem.class);
        when(item.getShortUrl()).thenReturn("https://example.com/short");
        when(item.getUrl()).thenReturn("https://example.com/long");
        when(response.getGoodsPromotionUrlGenerateResponse().getGoodsPromotionUrlList()).thenReturn(Collections.singletonList(item));
        return response;
    }

    @Test
    public void officialPromotionUsesAuthorizedUidAndNewGoodsSignRequest() throws Exception {
        PddDdkMemberAuthorityQueryResponse auth = mock(PddDdkMemberAuthorityQueryResponse.class, RETURNS_DEEP_STUBS);
        when(auth.getAuthorityQueryResponse().getBind()).thenReturn(1);
        when(client.syncInvoke(any(PddDdkMemberAuthorityQueryRequest.class))).thenReturn(auth);
        PddDdkGoodsPromotionUrlGenerateResponse links = promotion();
        when(client.syncInvoke(any(PddDdkGoodsPromotionUrlGenerateRequest.class))).thenReturn(links);
        PddWordVO word = sdk().getGoodsWordById(42L, "signed-product");
        assertEquals("https://example.com/short", word.getLink());
        assertNull(word.getWechatId());
        verify(client).syncInvoke(isA(PddDdkGoodsPromotionUrlGenerateRequest.class));
        PddDdkGoodsPromotionUrlGenerateRequest request = (PddDdkGoodsPromotionUrlGenerateRequest) mockingDetails(client).getInvocations().stream()
                .map(call -> call.getArguments()[0]).filter(PddDdkGoodsPromotionUrlGenerateRequest.class::isInstance)
                .findFirst().get();
        Map<String, String> params = request.getParamsMap();
        // custom_parameters 的 key 取自 pdd.paramPrefix（生产配置为 sfbuid），
        // value 是该用户的 uid，用于拼多多侧归单
        assertEquals("42", JSON.parseObject(params.get("custom_parameters")).getString("sfbuid"));
        assertTrue(params.get("goods_gen_url_param_list").contains("signed-product"));
        assertEquals("pid-test", params.get("p_id"));
    }

    @Test
    public void superRedPackageFailureFallsBackToOrdinaryPromotion() throws Exception {
        PddService service = sdk();
        config.setSuperRedPackage(1);
        PddDdkRpPromUrlGenerateResponse red = mock(PddDdkRpPromUrlGenerateResponse.class, RETURNS_DEEP_STUBS);
        when(red.getRpPromotionUrlGenerateResponse().getUrlList()).thenReturn(Collections.emptyList());
        when(client.syncInvoke(any(PddDdkRpPromUrlGenerateRequest.class))).thenReturn(red);
        PddDdkGoodsPromotionUrlGenerateResponse links = promotion();
        when(client.syncInvoke(any(PddDdkGoodsPromotionUrlGenerateRequest.class))).thenReturn(links);
        assertEquals("https://example.com/short", service.getGoodsWordById(null, "signed-product").getLink());
        verify(client).syncInvoke(isA(PddDdkRpPromUrlGenerateRequest.class));
        PddDdkRpPromUrlGenerateRequest request = (PddDdkRpPromUrlGenerateRequest) mockingDetails(client).getInvocations().stream()
                .map(call -> call.getArguments()[0]).filter(PddDdkRpPromUrlGenerateRequest.class::isInstance)
                .findFirst().get();
        assertEquals("23", request.getParamsMap().get("channel_type"));
        assertTrue(request.getParamsMap().get("diy_sp_red_packet_param").contains("signed-product"));
    }

    @Test
    public void listsReturnNormalizedFieldsAndPaginationSession() {
        DataokeService upstream = mock(DataokeService.class);
        GoodsListPddParam param = new GoodsListPddParam();
        PddSearchListVO response = JSON.parseObject("{\"code\":0,\"data\":{\"totalCount\":1,\"listId\":\"session\",\"goodsList\":[{\"goodsSign\":\"sign\",\"goodsName\":\"名称\",\"minGroupPrice\":39.9,\"couponDiscount\":10}]}}", PddSearchListVO.class);
        when(upstream.goodsListPdd(param)).thenReturn(response);
        DataokePDDController controller = new DataokePDDController();
        ReflectionTestUtils.setField(controller, "service", upstream);
        Map<String, Object> data = controller.getGoodList(param).getData();
        assertEquals("session", data.get("minId"));
        assertEquals(1, data.get("total"));
        Map<?, ?> item = (Map<?, ?>) ((List<?>)data.get("list")).get(0);
        assertEquals("sign", item.get("goodsId"));
        assertEquals(39.9, item.get("endPrice"));
        assertFalse(item.containsKey("minGroupPrice"));
    }

    @Test
    public void categoryCacheIsScopedByParentAndSorted() {
        DataokeService upstream = mock(DataokeService.class);
        RedisUtils cache = mock(RedisUtils.class);
        when(upstream.goodsCatePdd(anyInt())).thenReturn(JSONObject.parseObject("{\"code\":0,\"data\":[{\"id\":2,\"name\":\"乙\"},{\"id\":1,\"name\":\"甲\"}]}"));
        DataokePDDController controller = new DataokePDDController();
        ReflectionTestUtils.setField(controller, "service", upstream);
        ReflectionTestUtils.setField(controller, "redisUtils", cache);
        assertEquals(1, controller.getGoodList(0).getData().get(0).get("cid"));
        controller.getGoodList(1);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(cache, times(2)).get(keys.capture());
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }
}
