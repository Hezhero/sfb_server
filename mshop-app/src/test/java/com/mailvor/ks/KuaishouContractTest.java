package com.mailvor.ks;

import com.alibaba.fastjson.JSONObject;
import com.mailvor.modules.tk.param.GoodsListKsParam;
import com.mailvor.modules.tk.param.QueryKsOrderParam;
import com.mailvor.modules.tk.service.KuService;
import com.mailvor.modules.tk.vo.KsAnalyzeCodeVo;
import com.mailvor.modules.tk.vo.KsGoodsDetailVo;
import com.mailvor.modules.tk.vo.KsGoodsListVo;
import com.mailvor.modules.tk.vo.KsItemRatesUrlVo;
import com.mailvor.modules.tk.vo.KsOrderResVo;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 快手全链路契约测试：用 MockRestServiceServer 锁定五个好单库接口的路径、参数与成功码解析。
 * 官方规格见 .scratch/tri-platform-audit/issues/06-kuaishou.md（不要再联网推测）。
 */
public class KuaishouContractTest {

    private KuService service(RestTemplate http) {
        KuService service = new KuService();
        ReflectionTestUtils.setField(service, "restTemplate", http);
        ReflectionTestUtils.setField(service, "key", "test-apikey");
        return service;
    }

    @Test
    public void itemListSendsFiltersAndAcceptsCodeOne() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {
            assertEquals("/ks_item_list", request.getURI().getPath());
            // URI.getQuery() 会解码（中文变回"霸电"），断言编码值必须用 getRawQuery()
            String query = request.getURI().getRawQuery();
            assertTrue(query.contains("apikey=test-apikey"));
            assertTrue(query.contains("keyword=%E9%9C%B8%E7%94%B5"));
            assertTrue(query.contains("min_id=7"));
            assertTrue(query.contains("back=20"));
            assertTrue(query.contains("sort_type=2"));
            assertTrue(query.contains("min_price=10"));
            assertTrue(query.contains("max_price=99"));
            assertTrue(query.contains("min_sale=100"));
            assertTrue(query.contains("max_sale=999"));
        }).andRespond(withSuccess("{\"code\":1,\"msg\":\"success\",\"min_id\":8,\"data\":[{\"product_id\":\"P1\",\"itemtitle\":\"电池\",\"itemprice\":99,\"itemendprice\":29.9,\"itempic\":\"pic\",\"sales\":1200,\"ksmoney\":3.5,\"ksrates\":12}]}", MediaType.APPLICATION_JSON));
        GoodsListKsParam param = new GoodsListKsParam();
        param.setKeyword("霸电");
        param.setMinId(7);
        param.setBack(20);
        param.setSortType(2);
        param.setMinPrice(10D);
        param.setMaxPrice(99D);
        param.setMinSale(100);
        param.setMaxSale(999);
        KsGoodsListVo result = service(http).itemList(param);
        assertEquals(Integer.valueOf(1), result.getCode());
        assertEquals(Integer.valueOf(8), result.getMinId());
        assertEquals(1, result.getData().size());
        assertEquals("P1", result.getData().get(0).getProductId());
        assertEquals("电池", result.getData().get(0).getItemtitle());
        assertEquals(Double.valueOf(29.9D), result.getData().get(0).getItemendprice());
        assertEquals(Double.valueOf(3.5D), result.getData().get(0).getKsmoney());
        server.verify();
    }

    @Test
    public void itemDetailSendsProductIdAndAcceptsCode200WithGallery() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {
            assertEquals("/ks_item_detail", request.getURI().getPath());
            assertTrue(request.getURI().getQuery().contains("product_id=P1"));
        }).andRespond(withSuccess("{\"code\":200,\"msg\":\"success\",\"data\":{\"product_id\":\"P1\",\"itemtitle\":\"详情\",\"ks_images\":[\"g1\",\"g2\"],\"ks_detail_imgs\":[\"d1\"],\"brand_name\":\"品牌\"}}", MediaType.APPLICATION_JSON));
        KsGoodsDetailVo result = service(http).itemDetail("P1");
        assertEquals(Integer.valueOf(200), result.getCode());
        assertEquals("P1", result.getData().getProductId());
        assertEquals(2, result.getData().getKsImages().size());
        assertEquals("品牌", result.getData().getBrandName());
        server.verify();
    }

    @Test
    public void ratesUrlPostsFormWithChannelAndAcceptsCodeOne() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {
            assertEquals(HttpMethod.POST, request.getMethod());
            assertEquals("/ks_item_ratesurl", request.getURI().getPath());
            String body = new String(((MockClientHttpRequest) request).getBodyAsBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("apikey=test-apikey"));
            assertTrue(body.contains("product_id=P1"));
            assertTrue(body.contains("channel=uid42"));
        }).andRespond(withSuccess("{\"code\":1,\"msg\":\"success\",\"data\":{\"linkCode\":\"口令\",\"kwaiUrl\":\"https://kuaishou.com/p\",\"linkUrl\":\"https://s.url\"}}", MediaType.APPLICATION_JSON));
        KsItemRatesUrlVo result = service(http).itemRatesUrl("P1", "uid42");
        assertEquals(Integer.valueOf(1), result.getCode());
        assertEquals("口令", result.getData().getLinkCode());
        assertEquals("https://kuaishou.com/p", result.getData().getKwaiUrl());
        assertEquals("https://s.url", result.getData().getLinkUrl());
        server.verify();
    }

    @Test
    public void analyzeCodePostsFormAndReturnsProductId() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {
            assertEquals(HttpMethod.POST, request.getMethod());
            assertEquals("/ks_analyze_code", request.getURI().getPath());
            String body = new String(((MockClientHttpRequest) request).getBodyAsBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("apikey=test-apikey"));
            assertTrue(body.contains("content=%E5%8F%A3%E4%BB%A4"));
        }).andRespond(withSuccess("{\"code\":200,\"msg\":\"success\",\"data\":{\"product_id\":\"P9\"}}", MediaType.APPLICATION_JSON));
        KsAnalyzeCodeVo result = service(http).analyzeCode("口令");
        assertEquals(Integer.valueOf(200), result.getCode());
        assertEquals("P9", result.getData().getProductId());
        server.verify();
    }

    @Test
    public void orderListUsesUnixSecondsAndCursorAndAcceptsCodeOne() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {
            assertEquals("/ks_order_list", request.getURI().getPath());
            String query = request.getURI().getQuery();
            assertTrue(query.contains("min_id=0"));
            assertTrue(query.contains("back=100"));
            assertTrue(query.contains("start_date=1700000000"));
            assertTrue(query.contains("end_date=1700100000"));
            assertTrue(query.contains("date_type=1"));
            assertTrue(query.contains("state=0"));
        }).andRespond(withSuccess("{\"code\":1,\"msg\":\"success\",\"min_id\":55,\"data\":[{\"trade_id\":\"T1\",\"trade_parent_id\":\"TP1\",\"pay_price\":29.9,\"predict_money\":3.5,\"actual_money\":3.0,\"settled_status\":1,\"order_status\":1,\"channel_code\":\"uid42\",\"paid_time\":1700000100,\"earning_time\":1700000200}]}", MediaType.APPLICATION_JSON));
        QueryKsOrderParam param = new QueryKsOrderParam();
        param.setMinId(0);
        param.setBack(100);
        param.setStart(1700000000L);
        param.setEnd(1700100000L);
        param.setDateType(1);
        param.setState(0);
        KsOrderResVo result = service(http).orderList(param);
        assertEquals(Integer.valueOf(1), result.getCode());
        assertEquals(Integer.valueOf(55), result.getMinId());
        assertEquals(1, result.getData().size());
        assertEquals("T1", result.getData().get(0).getTradeId());
        assertEquals("uid42", result.getData().get(0).getChannelCode());
        server.verify();
    }

    @Test
    public void upstreamFailureCodeIsKeptForControllerToTranslate() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(request -> {}).andRespond(withSuccess("{\"code\":0,\"msg\":\"解析失败\"}", MediaType.APPLICATION_JSON));
        KsAnalyzeCodeVo result = service(http).analyzeCode("假口令");
        assertEquals(Integer.valueOf(0), result.getCode());
        server.verify();
    }
}
