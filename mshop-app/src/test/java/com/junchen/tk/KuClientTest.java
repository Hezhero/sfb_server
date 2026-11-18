package com.junchen.tk;

import com.junchen.modules.tk.service.KuClient;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * KuClient transport seam 测试。
 *
 * <p><b>为什么需要</b>：好单库同一上游被三种 HTTP 栈、四个域名、三套成功码各写一遍。
 * 新接入的快手/闲鱼/高佣三套接口把成功码判定散在 Controller 与 Service 两处。
 * KuClient 把「注入 apikey、发 GET/POST form、翻译成功码」收进一个 seam，
 * 调用方只处理业务。</p>
 */
public class KuClientTest {

    private static final String KEY = "test-apikey";

    @Test
    public void getInjectsApiKeyAndReturnsBody() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("apikey=" + KEY)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1,\"data\":{}}", MediaType.APPLICATION_JSON));

        KuClient client = new KuClient(http, KEY);
        String body = client.get("https://v3.api.haodanku.com/ks_item_list", null);
        assertEquals("{\"code\":1,\"data\":{}}", body);
        server.verify();
    }

    @Test
    public void getAppendsQueryParams() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("keyword=phone"),
                        org.hamcrest.Matchers.containsString("min_id=7"))))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        KuClient client = new KuClient(http, KEY);
        client.get("https://v3.api.haodanku.com/ks_item_list",
                java.util.Map.of("keyword", "phone", "min_id", "7"));
        server.verify();
    }

    @Test
    public void postFormSendsUrlEncodedBody() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/ks_item_ratesurl")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":1}", MediaType.APPLICATION_JSON));

        KuClient client = new KuClient(http, KEY);
        client.postForm("https://v3.api.haodanku.com/ks_item_ratesurl",
                java.util.Map.of("product_id", "P1"));
        server.verify();
    }

    @Test
    public void successCodeAcceptsOfficialOneAndTwoHundred() {
        assertEquals(true, KuClient.isSuccess(1));
        assertEquals(true, KuClient.isSuccess(200));
        assertEquals(false, KuClient.isSuccess(0));
        assertEquals(false, KuClient.isSuccess(null));
        assertEquals(false, KuClient.isSuccess(500));
    }
}
