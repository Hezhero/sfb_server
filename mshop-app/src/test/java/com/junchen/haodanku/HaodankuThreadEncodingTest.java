package com.junchen.haodanku;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.tk.param.ParseContentParam;
import com.junchen.modules.tk.service.KuService;
import com.junchen.modules.tk.util.HttpUtils;
import com.junchen.modules.tk.util.SignMD5Util;
import com.junchen.modules.tk.vo.TkParseCodeVO;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 工单 11：好单库线程安全与编码修复的回归测试。
 *
 * <p><b>接缝</b>：
 * <ol>
 *   <li>{@link KuService#formatDate(Date)} —— 签名日期格式化的纯函数接缝，
 *       并发调用必须一一对应地返回正确的日期字符串；捕获 SimpleDateFormat 的字段串扰。</li>
 *   <li>{@link KuService#postJson(String, String)} —— 可覆写的 HTTP 接缝，
 *       通过 {@code clipboard()}/{@code shortLink()} 公开方法观察实际发出的
 *       {@code date} 与 {@code sign}，验证每个请求体的签名与自身日期一致。</li>
 *   <li>{@link HttpUtils#doGet(String)} / {@link HttpUtils#doPost(String, String)} —— 本地
 *       HTTP 服务返回 UTF-8 中文，验证解码不乱码；不可达地址返回结构化错误而非空串。</li>
 * </ol>
 */
public class HaodankuThreadEncodingTest {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String TEST_APP_SECRET = "testSecret";
    private static HttpServer server;
    private static int port;

    @BeforeClass
    public static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        byte[] body = "{\"code\":200,\"msg\":\"成功\",\"data\":{\"title\":\"深海鱼油\"}}"
                .getBytes(StandardCharsets.UTF_8);
        server.createContext("/utf8", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---------- 1. 并发日期格式化 ----------

    @Test
    public void formatDateIsStableUnderConcurrentCallsWithDistinctDates() throws Exception {
        final int threads = 8;
        final int perThread = 500;
        final long base = parse("2025-01-01 00:00:00").getTime();
        final AtomicReference<String> firstFailure = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                final int id = t;
                pool.submit(() -> {
                    try {
                        start.await();
                        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN, Locale.CHINA);
                        for (int i = 0; i < perThread; i++) {
                            // 每个线程使用不同的日期，放大 SimpleDateFormat 内部字段串扰
                            Date d = new Date(base + (id * 1000L + i) * 60_000L);
                            String expected = sdf.format(d);
                            String actual = KuService.formatDate(d);
                            if (!expected.equals(actual)) {
                                firstFailure.compareAndSet(null,
                                        "expected=" + expected + " but actual=" + actual);
                                return;
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        firstFailure.compareAndSet(null, "interrupted");
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue("并发格式化出现乱码: " + firstFailure.get(),
                    pool.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertNull("并发格式化出现乱码: " + firstFailure.get(), firstFailure.get());
    }

    // ---------- 2. 并发签名：通过公开方法观察请求体 ----------

    /** 捕获 postJson 请求体的假 KuService，返回 code=200 的成功响应。 */
    private static final class CapturingKuService extends KuService {
        final List<String> bodies = Collections.synchronizedList(new ArrayList<>());
        CapturingKuService() {
            // 用不同变量名，避免遮蔽 KuService 的 private appId/appSecret 字段。
            // 必须通过 ReflectionTestUtils 设到父类字段上，否则 fastjson 序列化请求体时
            // app_id=null 会被省略，而签名原文仍拼了 "app_idnull"，导致签名与请求体对不上。
            ReflectionTestUtils.setField(this, "appId", "testApp");
            ReflectionTestUtils.setField(this, "appSecret", TEST_APP_SECRET);
        }

        @Override
        protected String postJson(String url, String json) {
            bodies.add(json);
            return "{\"code\":200,\"data\":{}}";
        }
    }

    private static Date parse(String s) throws ParseException {
        return new SimpleDateFormat(PATTERN, Locale.CHINA).parse(s);
    }

    @Test
    public void concurrentClipboardAndShortLinkProduceSignaturesMatchingTheirOwnDate() throws Exception {
        final CapturingKuService service = new CapturingKuService();
        final int threads = 8;
        final int perThread = 100;
        final AtomicReference<String> failure = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                final int id = t;
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            if ((id + i) % 2 == 0) {
                                service.clipboard(new ParseContentParam("内容-" + id + "-" + i));
                            } else {
                                service.shortLink("https://example.com/" + id + "/" + i);
                            }
                        }
                    } catch (Exception e) {
                        failure.compareAndSet(null, "调用异常: " + e);
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue("并发任务超时", pool.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertNull(failure.get());

        List<String> snapshot = new ArrayList<>(service.bodies);
        assertEquals("每个请求都应发出 JSON", threads * perThread, snapshot.size());
        for (String body : snapshot) {
            JSONObject json = JSON.parseObject(body);
            String date = json.getString("date");
            String sign = json.getString("sign");
            assertNotNull("缺少 date 字段: " + body, date);
            assertNotNull("缺少 sign 字段: " + body, sign);
            // 日期必须是合法的 yyyy-MM-dd HH:mm:ss，且不含乱码
            assertTrue("date 格式非法: " + date,
                    date.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
            // 按签名原文独立重算 MD5，验证签名与自身日期一致
            StringBuilder raw = new StringBuilder();
            List<String> keys = new ArrayList<>(json.keySet());
            keys.remove("sign");
            Collections.sort(keys);
            for (String k : keys) {
                raw.append(k).append(json.getString(k));
            }
            raw.append(TEST_APP_SECRET);
            String expected = SignMD5Util.MD5(raw.toString()).toUpperCase();
            assertEquals("签名与日期不匹配（疑似 SimpleDateFormat 串扰）: " + body,
                    expected, sign);
        }
    }

    // ---------- 3. 中文 UTF-8 解码 ----------

    @Test
    public void httpUtilsDecodesChineseUtf8InBothGetAndPost() {
        String url = "http://127.0.0.1:" + port + "/utf8";
        String get = HttpUtils.doGet(url);
        String post = HttpUtils.doPost(url, "{}");
        assertEquals("doGet 中文乱码", "成功", JSON.parseObject(get).getString("msg"));
        assertEquals("doGet 中文商品名乱码", "深海鱼油",
                JSON.parseObject(get).getJSONObject("data").getString("title"));
        assertEquals("doPost 中文乱码", "成功", JSON.parseObject(post).getString("msg"));
        assertEquals("doPost 中文商品名乱码", "深海鱼油",
                JSON.parseObject(post).getJSONObject("data").getString("title"));
        assertEquals("doGet/doPost 解码行为应一致", get, post);
    }

    // ---------- 4. 失败路径：返回结构化错误，不 NPE ----------

    @Test
    public void httpUtilsReturnsStructuredErrorOnUnreachableUrl() {
        String result = HttpUtils.doPost("http://127.0.0.1:1/unreachable", "{}");
        assertNotNull(result);
        assertFalse("失败时不得返回空串", result.isEmpty());
        JSONObject error = JSON.parseObject(result);
        assertEquals("结构化错误应带 code", Integer.valueOf(500), error.getInteger("code"));
        assertNotNull("结构化错误应带可读 msg", error.getString("msg"));
    }

    @Test
    public void clipboardDoesNotThrowNpeWhenUpstreamFails() {
        KuService service = new KuService() {
            @Override
            protected String postJson(String url, String json) {
                return HttpUtils.doPost("http://127.0.0.1:1/unreachable", json);
            }
        };
        ReflectionTestUtils.setField(service, "appId", "testApp");
        ReflectionTestUtils.setField(service, "appSecret", "testSecret");
        TkParseCodeVO vo = service.clipboard(new ParseContentParam("内容"));
        assertNotNull("上游失败时应返回 VO 而不是 null/NPE", vo);
        assertNotEquals("应返回失败码", Integer.valueOf(0), vo.getCode());
    }

    @Test
    public void shortLinkDoesNotThrowNpeWhenUpstreamFails() throws Exception {
        KuService service = new KuService() {
            @Override
            protected String postJson(String url, String json) {
                return HttpUtils.doPost("http://127.0.0.1:1/unreachable", json);
            }
        };
        ReflectionTestUtils.setField(service, "appId", "testApp");
        ReflectionTestUtils.setField(service, "appSecret", "testSecret");
        JSONObject result = service.shortLink("https://example.com/a");
        assertNotNull("上游失败时应返回 JSON 而不是 null/NPE", result);
    }
}
