package com.junchen.modules.tk.util;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 好单库 HTTP 工具。
 *
 * <p>统一使用 UTF-8 解码响应与编码请求（好单库返回 UTF-8，原 doPost 用 GBK 导致中文乱码）。
 * 失败时返回结构化错误 JSON（code=500 + msg），不再返回空串让调用方 parseObject("") 得到
 * null 后 NPE；异常统一走 slf4j 日志，不再 printStackTrace。</p>
 */
public class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    /** 上游失败时返回的结构化错误，保证调用方拿到可解析的 JSON 而不是空串。 */
    private static String errorJson(String message) {
        JSONObject error = new JSONObject();
        error.put("code", 500);
        error.put("msg", message == null ? "好单库请求失败" : message);
        return error.toJSONString();
    }

    public static String doGet(String url) {
        HttpURLConnection client = null;
        InputStream is = null;
        BufferedReader br = null;
        StringBuffer result = new StringBuffer();
        try {
            URL httpUrl = new URL(url);
            client = (HttpURLConnection) httpUrl.openConnection();
            client.setRequestMethod("GET");
            client.setReadTimeout(15000);
            client.connect();
            if (client.getResponseCode() == 200) {
                is = client.getInputStream();
                if (null != is) {
                    br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String temp;
                    while (null != (temp = br.readLine())) {
                        result.append(temp);
                    }
                    return result.toString();
                }
                log.error("好单库 GET 请求未返回内容: {}", url);
                return errorJson("好单库 GET 请求未返回内容");
            }
            log.error("好单库 GET 请求失败, status={}, url={}", client.getResponseCode(), url);
            return errorJson("好单库 GET 请求失败, status=" + client.getResponseCode());
        } catch (Exception e) {
            log.error("好单库 GET 请求异常, url=" + url, e);
            return errorJson("好单库 GET 请求异常: " + e.getMessage());
        } finally {
            closeQuietly(br);
            closeQuietly(is);
            if (client != null) {
                client.disconnect();
            }
        }
    }

    public static String doPost(String httpUrl, String jsonStr) {
        StringBuffer result = new StringBuffer();
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            URL url = new URL(httpUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            out = connection.getOutputStream();
            out.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            out.flush();
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                if (null != is) {
                    br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String temp;
                    while (null != (temp = br.readLine())) {
                        result.append(temp);
                    }
                    return result.toString();
                }
                log.error("好单库 POST 请求未返回内容: {}", httpUrl);
                return errorJson("好单库 POST 请求未返回内容");
            }
            log.error("好单库 POST 请求失败, status={}, url={}", connection.getResponseCode(), httpUrl);
            return errorJson("好单库 POST 请求失败, status=" + connection.getResponseCode());
        } catch (Exception e) {
            log.error("好单库 POST 请求异常, url=" + httpUrl, e);
            return errorJson("好单库 POST 请求异常: " + e.getMessage());
        } finally {
            closeQuietly(br);
            closeQuietly(out);
            closeQuietly(is);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.warn("关闭 HTTP 资源失败", e);
            }
        }
    }
}
