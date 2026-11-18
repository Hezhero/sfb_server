package com.junchen.tblive;

import com.junchen.modules.tk.param.QueryTbLiveOrderParam;
import com.junchen.modules.tk.service.KuService;
import com.junchen.modules.tk.vo.TbLiveGoodsVo;
import com.junchen.modules.tk.vo.TbLiveLinkVo;
import com.junchen.modules.tk.vo.TbLiveOrderResVo;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 淘宝更高佣三件套契约测试：锁定三个好单库接口的路径、参数与成功码解析。
 * 官方规格见 .scratch/tri-platform-audit/issues/08-taobao-high-commission.md。
 */
public class TbLiveContractTest {

    private KuService service(RestTemplate http) {
        KuService service = new KuService();
        ReflectionTestUtils.setField(service, "restTemplate", http);
        ReflectionTestUtils.setField(service, "key", "test-apikey");
        return service;
    }

    @Test
    public void goodsQuerySendsObjectIdAndParsesOfficialFields() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        String body = "{\"code\":1,\"msg\":\"success\",\"data\":{"
                + "\"aduser_id\":\"AD1\",\"item_title\":\"高佣商品\",\"auction_price\":99.0,"
                + "\"itemsale\":500,\"object_pic\":\"pic\",\"commission_rates\":30.5,"
                + "\"commission\":28.8,\"promotion_id\":\"P1\"}}";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/tblive_goods_query?apikey=test-apikey&object_id=ITEM1")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TbLiveGoodsVo vo = service(http).tbLiveGoodsQuery("ITEM1");
        assertTrue(vo.isOk());
        assertEquals("AD1", vo.getData().getAduserId());
        assertEquals("高佣商品", vo.getData().getItemTitle());
        assertEquals(Double.valueOf(30.5), vo.getData().getCommissionRates());
        assertEquals("P1", vo.getData().getPromotionId());
        server.verify();
    }

    @Test
    public void linkSendsPromotionIdAndParsesDeepLink() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        String body = "{\"code\":1,\"msg\":\"success\",\"data\":{\"deep_link\":\"https://tb.cn/xxx\"}}";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/get_tblive_link")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TbLiveLinkVo vo = service(http).tbLiveLink("P1", 2, "12345");
        assertTrue(vo.isOk());
        assertEquals("https://tb.cn/xxx", vo.getData().getDeepLink());
        server.verify();
    }

    @Test
    public void orderListSendsUnixSecondsAndParsesOrders() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        String body = "{\"code\":200,\"msg\":\"success\",\"min_id\":8,\"data\":[{"
                + "\"trade_id\":\"T1\",\"trade_parent_id\":\"PT1\",\"item_title\":\"商品\","
                + "\"pay_price\":99.0,\"predict_money\":30.0,\"actual_money\":28.0,"
                + "\"paid_time\":\"1700000000\",\"earning_time\":\"1700100000\","
                + "\"order_status\":3,\"settled_status\":1,\"channel_code\":\"12345\"}]}";
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/tb_live_order_list"),
                        org.hamcrest.Matchers.containsString("start_date=1700000000"),
                        org.hamcrest.Matchers.containsString("end_date=1700100000"),
                        org.hamcrest.Matchers.containsString("date_type=1"))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        QueryTbLiveOrderParam param = new QueryTbLiveOrderParam();
        param.setStartTime(1700000000L);
        param.setEndTime(1700100000L);
        TbLiveOrderResVo vo = service(http).tbLiveOrderList(param);
        assertEquals(Integer.valueOf(200), vo.getCode());
        assertEquals(Integer.valueOf(8), vo.getMinId());
        assertEquals(1, vo.getData().size());
        assertEquals("T1", vo.getData().get(0).getTradeId());
        server.verify();
    }

    @Test
    public void errorCodeIsExposedNotThrown() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/tblive_goods_query")))
                .andRespond(withSuccess("{\"code\":500,\"msg\":\"参数错误\"}", MediaType.APPLICATION_JSON));

        TbLiveGoodsVo vo = service(http).tbLiveGoodsQuery("BAD");
        assertFalse(vo.isOk());
        assertEquals("参数错误", vo.getMsg());
        server.verify();
    }
}
