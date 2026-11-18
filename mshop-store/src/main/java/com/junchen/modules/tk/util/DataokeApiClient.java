package com.junchen.modules.tk.util;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.junchen.api.MshopException;

import java.util.Map;
import java.util.TreeMap;

import static com.junchen.modules.tk.util.DataokeApi.GOODS_SEARCH;

/**
 * @projectName:dtk-items-openapi
 * @createTime: 2019年11月28日14:13:21
 * @description:
 */
public class DataokeApiClient {

    /** 连接超时：5s。上游不可达时快速失败，不占住 Tomcat 线程 */
    public static final int CONNECT_TIMEOUT_MILLIS = 5000;
    /** 读取超时：15s。上游挂起最多占用 15s，避免雪崩 */
    public static final int READ_TIMEOUT_MILLIS = 15000;

    /**
     * HTTP 执行接缝。默认走 hutool；单测注入假实现以断言超时、模拟超时/非 JSON。
     */
    public interface RequestExecutor {
        String execute(HttpRequest request);
    }

    private static final RequestExecutor DEFAULT_EXECUTOR = new RequestExecutor() {
        @Override
        public String execute(HttpRequest request) {
            return request.execute().body();
        }
    };

    private static volatile RequestExecutor executor = DEFAULT_EXECUTOR;

    /** 仅供单测使用，生产代码不要调用 */
    public static void setExecutorForTest(RequestExecutor testExecutor) {
        executor = (testExecutor == null) ? DEFAULT_EXECUTOR : testExecutor;
    }

    public static RequestExecutor getExecutor() {
        return executor;
    }

    public static String sendReq(String url, String secret, Map<String, Object> paraMap){
        validateCommon(url, secret, paraMap);

        // 签名基于原始参数值计算（与官方 SDK 一致）。
        // 传输时的 URL 编码统一交由 HttpRequest.form 完成（内部只会编码一次），
        // 切勿在此再手动 URLEncoder.encode，否则会造成双重编码，服务端解出的值与加签时的原始值不一致，导致验签失败。
        paraMap.put("sign", SignMD5Util.getSignStr(paraMap, secret));

        HttpRequest request = HttpRequest.get(url)
                .form(paraMap)
                .setConnectionTimeout(CONNECT_TIMEOUT_MILLIS)
                .setReadTimeout(READ_TIMEOUT_MILLIS);

        String data;
        try {
            data = executor.execute(request);
        } catch (Exception e) {
            throw translateHttpException(e);
        }
        return validateResponse(data);
    }

    public static String sendReq(String url, String key, String secret, String version, Map<String, Object> paraMap) {
        if (isBlank(key)) {
            throw new MshopException("大淘客 appKey 不能为空，请检查 dataoke.key 配置");
        }
        if (paraMap == null) {
            throw new MshopException("大淘客请求参数不能为空");
        }
        paraMap.put("version", version);
        paraMap.put("appKey", key);
        return DataokeApiClient.sendReq(url, secret, paraMap);
    }

    /** 前置校验抛业务异常，不再返回裸中文串（调用方一律 JSON.parseObject，裸串会炸 JSONException） */
    private static void validateCommon(String url, String secret, Map<String, Object> paraMap) {
        if (isBlank(url)) {
            throw new MshopException("大淘客请求地址不能为空");
        }
        if (isBlank(secret)) {
            throw new MshopException("大淘客 secret 不能为空，请检查 dataoke.secret 配置");
        }
        if (paraMap == null || paraMap.isEmpty()) {
            throw new MshopException("大淘客请求参数不能为空");
        }
        Object appKey = paraMap.get("appKey");
        if (appKey != null && isBlank(String.valueOf(appKey))) {
            throw new MshopException("大淘客 appKey 不能为空，请检查 dataoke.key 配置");
        }
    }

    /** 上游异常中文化：区分超时与一般网络失败，绝不把原始异常/堆栈透给前端 */
    private static MshopException translateHttpException(Exception e) {
        String raw = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        boolean timeout = (e instanceof java.net.SocketTimeoutException)
                || raw.contains("timeout") || raw.contains("timed out");
        if (timeout) {
            return new MshopException("大淘客接口请求超时，请稍后重试");
        }
        if (e instanceof HttpException) {
            return new MshopException("大淘客接口调用失败，请检查网络");
        }
        return new MshopException("大淘客接口调用失败，请检查网络");
    }

    /** 上游返回非 JSON 或空串时转业务异常，避免调用方 JSON.parseObject 抛 JSONException */
    private static String validateResponse(String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new MshopException("大淘客返回为空");
        }
        try {
            JSON.parseObject(data);
        } catch (JSONException e) {
            throw new MshopException("大淘客返回格式异常");
        }
        return data;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 本地调试入口：从命令行参数 / 环境变量读取凭据，不在源码中硬编码。
     * 用法：main <appKey> <appSecret>，或先 export DATAOKE_KEY / DATAOKE_SECRET。
     */
    public static void main(String[] args) {
        String url = GOODS_SEARCH.getUrl();//热力主播推荐

        String appKey = args != null && args.length > 0 ? args[0] : System.getenv("DATAOKE_KEY");
        String appSecret = args != null && args.length > 1 ? args[1] : System.getenv("DATAOKE_SECRET");
        if (isBlank(appKey) || isBlank(appSecret)) {
            System.err.println("请提供大淘客凭据：main <appKey> <appSecret> 或设置 DATAOKE_KEY / DATAOKE_SECRET 环境变量");
            return;
        }
        TreeMap<String, Object> paraMap = new TreeMap<>();
        paraMap.put("version", "v2.1.2");
        paraMap.put("appKey", appKey);
        paraMap.put("pageId", "1");
        paraMap.put("pageSize", "20");
        paraMap.put("keyWords", "抽纸");
        long start = System.currentTimeMillis();
        String data = DataokeApiClient.sendReq(url, appSecret, paraMap);
        System.out.println("耗时："+ (System.currentTimeMillis() - start));
        System.out.println(data);

    }

}
