package com.mailvor.dataoke;

import cn.hutool.http.HttpRequest;
import com.mailvor.api.MshopException;
import com.mailvor.modules.tk.util.DataokeApiClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * 大淘客主链路超时与错误处理契约测试（工单 12）。
 *
 * <p><b>接缝</b>：{@link DataokeApiClient#sendReq(String, String, String, String, TreeMap)}
 * 这一公开入口。hutool 的静态 HTTP 无法在单测里指向本地慢服务，因此通过
 * {@link DataokeApiClient#setExecutorForTest(RequestExecutor)} 注入一个假执行器：
 * 断言送出的 HttpRequest 带有超时，并模拟上游抛超时异常/返回非 JSON。
 * 真实 socket 行为不在单测覆盖范围内。</p>
 *
 * <p>错误分支统一以 {@link MshopException} 呈现，杜绝裸中文字符串被
 * {@code JSON.parseObject} 解析成 {@code JSONException}，也不把上游原始异常透给前端。</p>
 */
public class DataokeApiClientErrorTest {

    private DataokeApiClient.RequestExecutor original;

    @Before
    public void setUp() {
        original = DataokeApiClient.getExecutor();
    }

    @After
    public void tearDown() {
        DataokeApiClient.setExecutorForTest(original);
    }

    /** hutool 5.5.x/5.7.x 的超时字段名一致；反射读取私有字段以断言请求确实带了超时 */
    private static int intField(HttpRequest request, String field) {
        Object value = ReflectionTestUtils.getField(request, field);
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    private static TreeMap<String, Object> params() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        map.put("pageId", "1");
        return map;
    }

    // ---------------------------------------------------------------
    // 前置校验：配置缺失时返回结构化业务异常，不发请求
    // ---------------------------------------------------------------

    @Test
    public void blankConfigThrowsBusinessExceptionWithoutSendingRequest() {
        final int[] calls = {0};
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                calls[0]++;
                return "{}";
            }
        });

        assertBlankRejected(null, "secret", "appKey 不能为空");
        assertBlankRejected("", "secret", "appKey 不能为空");
        assertBlankRejected("   ", "secret", "appKey 不能为空");
        assertBlankRejected("key", null, "secret 不能为空");
        assertBlankRejected("key", "", "secret 不能为空");
        assertBlankRejected("key", "   ", "secret 不能为空");

        assertEquals("配置为空时绝不能发出上游请求", 0, calls[0]);
    }

    private void assertBlankRejected(String key, String secret, String expectedMessage) {
        try {
            DataokeApiClient.sendReq("https://openapi.dataoke.com/api/x", key, secret, "v1", params());
            fail("配置为空必须抛出业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(expectedMessage));
            // 结构化错误：不能是裸中文句子被上层 JSON.parseObject 炸掉
            assertFalse(expected.getMessage(), expected.getMessage().startsWith("{"));
        }
    }

    // ---------------------------------------------------------------
    // 超时：所有 HTTP 请求必须带明确的连接/读取超时
    // ---------------------------------------------------------------

    @Test
    public void everyRequestCarriesConnectAndReadTimeout() {
        final int[] connectTimeout = {-1};
        final int[] readTimeout = {-1};
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                connectTimeout[0] = intField(request, "connectionTimeout");
                readTimeout[0] = intField(request, "readTimeout");
                return "{\"code\":0,\"data\":{}}";
            }
        });

        String data = DataokeApiClient.sendReq(
                "https://openapi.dataoke.com/api/x", "appkey", "secret", "v1", params());
        assertNotNull(data);

        // 连接 5s、读取 15s：上游挂起最多占用线程 15s，不会拖死 Tomcat
        assertEquals("连接超时必须为 5s", DataokeApiClient.CONNECT_TIMEOUT_MILLIS, connectTimeout[0]);
        assertEquals("读取超时必须为 15s", DataokeApiClient.READ_TIMEOUT_MILLIS, readTimeout[0]);
        assertEquals(5000, connectTimeout[0]);
        assertEquals(15000, readTimeout[0]);
    }

    @Test
    public void timeoutIsTranslatedToChineseBusinessException() {
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                throw new cn.hutool.http.HttpException("Read timed out");
            }
        });

        try {
            DataokeApiClient.sendReq("https://openapi.dataoke.com/api/x", "appkey", "secret", "v1", params());
            fail("超时必须转换为业务异常");
        } catch (MshopException expected) {
            String msg = expected.getMessage();
            assertTrue(msg, msg.contains("超时") || msg.contains("网络"));
            assertFalse("不能暴露上游原始异常", msg.contains("Read timed out"));
            assertFalse("不能暴露上游原始异常类名", msg.contains("HttpException"));
        }
    }

    // ---------------------------------------------------------------
    // 上游非 JSON：转成业务异常，不能把裸字符串丢给调用方
    // ---------------------------------------------------------------

    @Test
    public void nonJsonResponseIsTranslatedToBusinessException() {
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                return "<html>502 Bad Gateway</html>";
            }
        });

        try {
            DataokeApiClient.sendReq("https://openapi.dataoke.com/api/x", "appkey", "secret", "v1", params());
            fail("非 JSON 响应必须转换为业务异常");
        } catch (MshopException expected) {
            String msg = expected.getMessage();
            assertTrue(msg, msg.contains("格式") || msg.contains("JSON"));
            assertFalse("不能回显上游原始 HTML", msg.contains("<html>"));
        }
    }

    @Test
    public void blankResponseIsTranslatedToBusinessException() {
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                return "   ";
            }
        });

        try {
            DataokeApiClient.sendReq("https://openapi.dataoke.com/api/x", "appkey", "secret", "v1", params());
            fail("空响应必须转换为业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("空"));
        }
    }

    @Test
    public void validJsonResponseIsReturnedUnchanged() {
        final String json = "{\"code\":0,\"msg\":\"成功\",\"data\":{}}";
        DataokeApiClient.setExecutorForTest(new DataokeApiClient.RequestExecutor() {
            @Override
            public String execute(HttpRequest request) {
                return json;
            }
        });

        assertEquals(json,
                DataokeApiClient.sendReq("https://openapi.dataoke.com/api/x", "appkey", "secret", "v1", params()));
    }
}
