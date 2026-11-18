package com.junchen.idle;

import com.junchen.modules.tk.param.QueryIdleOrderParam;
import com.junchen.modules.tk.service.KuService;
import com.junchen.modules.tk.vo.IdleGoodsVo;
import com.junchen.modules.tk.vo.IdleLinkVo;
import com.junchen.modules.tk.vo.IdleOrderResVo;
import com.junchen.modules.tk.vo.IdleRechargeVo;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 闲鱼链路契约测试：锁定五个好单库普通接口的路径、参数与解析。
 * 官方规格见 .scratch/tri-platform-audit/issues/07-xianyu.md。
 */
public class IdleContractTest {

    private KuService service(RestTemplate http) {
        KuService service = new KuService();
        ReflectionTestUtils.setField(service, "restTemplate", http);
        ReflectionTestUtils.setField(service, "key", "test-apikey");
        return service;
    }

    @Test
    public void goodsListSendsKeywordAndParsesStatus() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        String body = "{\"code\":1,\"msg\":\"success\",\"min_id\":5,\"data\":[{"
                + "\"item_id\":\"I1\",\"item_title\":\"二手手机\",\"original_price\":1000,"
                + "\"image_urls\":[\"pic\"],\"reserve_price\":800,\"status\":\"promoting\","
                + "\"commission_rate\":5.5,\"estimated_commission\":44.0}]}";
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/idle_goods_query"),
                        org.hamcrest.Matchers.containsString("keyword="))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        IdleGoodsVo vo = service(http).idleGoodsList("手机", 3);
        assertTrue(vo.isOk());
        assertEquals(Integer.valueOf(5), vo.getMinId());
        assertEquals("I1", vo.getData().get(0).getItemId());
        assertEquals("promoting", vo.getData().get(0).getStatus());
        server.verify();
    }

    @Test
    public void goodsDetailUsesPlainItemIdWhenItemIdBlank() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("plain_itemid=12345")))
                .andRespond(withSuccess("{\"code\":1,\"data\":[{\"item_id\":\"I2\"}]}", MediaType.APPLICATION_JSON));

        IdleGoodsVo vo = service(http).idleGoodsDetail(null, "12345");
        assertTrue(vo.isOk());
        assertEquals("I2", vo.getData().get(0).getItemId());
        server.verify();
    }

    @Test
    public void rechargeSendsTypeAndParses() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/xy_prepaid_recharge"),
                        org.hamcrest.Matchers.containsString("type=2"))))
                .andRespond(withSuccess("{\"code\":1,\"data\":[{\"itemid\":\"R1\",\"itemendprice\":95.0}]}", MediaType.APPLICATION_JSON));

        IdleRechargeVo vo = service(http).idleRecharge(2, 1, 20);
        assertTrue(vo.isOk());
        assertEquals("R1", vo.getData().get(0).getItemid());
        server.verify();
    }

    @Test
    public void linkSendsMaterialTypeAndChannel() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/idle_general_link")))
                .andRespond(withSuccess("{\"code\":1,\"data\":{\"short_url\":\"https://x.cn/a\",\"short_tpwd\":\"口令\"}}", MediaType.APPLICATION_JSON));

        IdleLinkVo vo = service(http).idleGeneralLink(1, "I1", null, null, "9988");
        assertTrue(vo.isOk());
        assertEquals("https://x.cn/a", vo.getData().getShortUrl());
        assertEquals("口令", vo.getData().getShortTpwd());
        server.verify();
    }

    @Test
    public void orderListSendsUnixSecondsAndCursor() throws Exception {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        String body = "{\"code\":200,\"min_id\":9,\"data\":[{"
                + "\"trade_id\":\"T1\",\"pay_price\":99.0,\"predict_money\":5.0,"
                + "\"actual_money\":4.5,\"paid_time\":\"1700000000\","
                + "\"order_status\":2,\"channel_code\":\"9988\"}]}";
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("/idle_order_list"),
                        org.hamcrest.Matchers.containsString("start_date=1700000000"),
                        org.hamcrest.Matchers.containsString("min_id=1"))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        QueryIdleOrderParam param = new QueryIdleOrderParam();
        param.setStartTime(1700000000L);
        param.setEndTime(1700100000L);
        IdleOrderResVo vo = service(http).idleOrderList(param);
        assertEquals(Integer.valueOf(200), vo.getCode());
        assertEquals(Integer.valueOf(9), vo.getMinId());
        assertEquals("T1", vo.getData().get(0).getTradeId());
        server.verify();
    }
}
