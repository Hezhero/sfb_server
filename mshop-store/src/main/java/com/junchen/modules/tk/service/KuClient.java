package com.junchen.modules.tk.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好单库 transport seam：统一 apikey 注入、GET/POST-form 发送与成功码判定。
 *
 * <p><b>为什么需要</b>：同一上游被三种 HTTP 栈、四个域名、三套成功码各写一遍；
 * 新接入的快手/闲鱼/高佣三套接口把成功码判定散在 Service 与 Controller 两处
 * （{@code isKsSuccess} 与 {@code requireSuccess}）。KuClient 把这段收进一个 seam，
 * 让调用方只处理业务，且 HTTP 栈替换只动一处。</p>
 *
 * <p>注意：本类只服务新接入的接口；历史方法（抖音 CMS、外卖等）保持原样，
 * 避免一次性改动 28 处调用引入回归。</p>
 */
public class KuClient {

    private static final String API_KEY_PARAM = "apikey";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public KuClient(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /** GET：自动注入 apikey 并拼接额外查询参数 */
    public String get(String url, Map<String, ?> params) {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put(API_KEY_PARAM, apiKey);
        if (params != null) {
            params.forEach((k, v) -> { if (v != null) all.put(k, v); });
        }
        String query = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + encode(String.valueOf(e.getValue())))
                .collect(Collectors.joining("&"));
        ResponseEntity<String> re = restTemplate.getForEntity(
                URI.create(url + (url.contains("?") ? "&" : "?") + query), String.class);
        return re == null ? null : re.getBody();
    }

    /** POST form-data：自动注入 apikey */
    public String postForm(String url, Map<String, ?> params) {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put(API_KEY_PARAM, apiKey);
        if (params != null) {
            params.forEach((k, v) -> { if (v != null) all.put(k, v); });
        }
        String body = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + encode(String.valueOf(e.getValue())))
                .collect(Collectors.joining("&"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> re = restTemplate.postForEntity(
                URI.create(url), new HttpEntity<>(body, headers), String.class);
        return re == null ? null : re.getBody();
    }

    /** 官方成功码：1 或 200。唯一 owner。 */
    public static boolean isSuccess(Integer code) {
        return code != null && (code == 1 || code == 200);
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
