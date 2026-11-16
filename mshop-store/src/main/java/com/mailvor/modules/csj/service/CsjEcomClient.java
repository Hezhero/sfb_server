/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.csj.service;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mailvor.api.MshopException;
import com.mailvor.modules.csj.config.CsjEcomConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 穿山甲电商联盟 API 客户端（依官方《抖音电商CPS接口文档》28733 实现）
 *
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>权限</th></tr>
 *   <tr><td>商品列表</td><td>/product/search</td><td>需流量备案</td></tr>
 *   <tr><td>商品详情</td><td>/product/detail</td><td>需流量备案</td></tr>
 *   <tr><td>商品类目</td><td>/product/category</td><td>需流量备案</td></tr>
 *   <tr><td>商品转链</td><td>/product/link</td><td>需流量备案</td></tr>
 *   <tr><td>订单查询</td><td>/order/search</td><td>实名 + 角色绑定</td></tr>
 * </table>
 *
 * <p><b>订单接口是游标分页</b>：首次 cursor 传 "0"，之后传上次返回的 cursor，
 * 返回 cursor 为 "" 表示没有更多。size 取值 [1,50]，start_time/end_time 为
 * <b>秒级时间戳</b>（北京时间），且间隔不得超过 90 天。</p>
 *
 * @author mailvor
 */
@Slf4j
@Component
@AllArgsConstructor
public class CsjEcomClient {

    private static final String PATH_ORDER_SEARCH = "/order/search";
    private static final String PATH_PRODUCT_SEARCH = "/product/search";
    private static final String PATH_PRODUCT_DETAIL = "/product/detail";
    private static final String PATH_PRODUCT_CATEGORY = "/product/category";
    private static final String PATH_PRODUCT_LINK = "/product/link";

    /** 商品分销订单 */
    public static final int ORDER_TYPE_PRODUCT = 1;
    /** 直播间分销订单 */
    public static final int ORDER_TYPE_LIVE = 2;

    /** 商品搜索无结果，属正常业务结果而非错误 */
    private static final long CODE_NO_RESULT = 200001L;
    /** 暂无业务权限（未完成流量备案） */
    private static final long CODE_NO_PERMISSION = 100011L;

    private final CsjEcomConfig config;

    // ------------------------------------------------------------------
    // 订单
    // ------------------------------------------------------------------

    /**
     * 按时间区间游标拉取订单
     *
     * @param cursor    首次传 "0"，之后传上次返回的 cursor
     * @param size      本次拉取数量，取值 [1,50]
     * @param startTime 支付开始时间（秒级时间戳，北京时间）
     * @param endTime   支付结束时间（秒级时间戳），与 startTime 间隔不超过 90 天
     * @param orderType 1-商品分销 2-直播间分销 3-活动类型
     * @return data 对象，含 cursor 与 orders
     */
    public JSONObject orderSearchPage(String cursor, int size, long startTime, long endTime,
                                      int orderType, String timeType) {
        Map<String, Object> param = new HashMap<>(8);
        param.put("size", Math.min(Math.max(size, 1), 50));
        param.put("cursor", (cursor == null || cursor.trim().isEmpty()) ? "0" : cursor.trim());
        param.put("start_time", startTime);
        param.put("end_time", endTime);
        param.put("order_type", orderType);
        if (timeType != null && !timeType.trim().isEmpty()) {
            param.put("time_type", timeType.trim());
        }
        JSONObject root = post(PATH_ORDER_SEARCH, param, true);
        if (root == null) {
            return new JSONObject();
        }
        Object data = root.get("data");
        return data instanceof JSONObject ? (JSONObject) data : new JSONObject();
    }

    /**
     * 按订单号精确查询（最多 10 个）
     */
    public JSONObject orderSearchByIds(List<String> orderIds, int orderType) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new MshopException("订单号不能为空");
        }
        if (orderIds.size() > 10) {
            throw new MshopException("单次最多查询 10 个订单号");
        }
        Map<String, Object> param = new HashMap<>(4);
        param.put("order_ids", orderIds);
        param.put("order_type", orderType);
        JSONObject root = post(PATH_ORDER_SEARCH, param, true);
        if (root == null) {
            return new JSONObject();
        }
        Object data = root.get("data");
        return data instanceof JSONObject ? (JSONObject) data : new JSONObject();
    }

    // ------------------------------------------------------------------
    // 商品
    // ------------------------------------------------------------------

    /**
     * 商品列表
     *
     * @param title    商品关键词（对应 data.title，不是 keyword）
     * @param page     页码，从 1 开始
     * @param pageSize 每页数量，取值 [1,20]
     */
    public JSONArray productSearch(String title, int page, Integer pageSize) {
        int size = pageSize == null ? config.getPageSize() : pageSize;
        size = Math.min(Math.max(size, 1), 20);
        Map<String, Object> param = new HashMap<>(8);
        if (title != null && !title.trim().isEmpty()) {
            param.put("title", title.trim());
        }
        param.put("page", Math.max(page, 1));
        param.put("page_size", size);
        JSONObject root = post(PATH_PRODUCT_SEARCH, param, false);
        return extractList(root, "products");
    }

    /**
     * 商品详情
     *
     * @param productIds 商品 ID 列表
     */
    public JSONArray productDetail(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new MshopException("商品 ID 不能为空");
        }
        Map<String, Object> param = new HashMap<>(4);
        param.put("product_ids", productIds);
        JSONObject root = post(PATH_PRODUCT_DETAIL, param, false);
        return extractList(root, "products");
    }

    /**
     * 商品类目
     *
     * @param parentId 上级类目；查一级类目传 0 或不传
     */
    public JSONArray productCategory(Integer parentId) {
        Map<String, Object> param = new HashMap<>(4);
        param.put("parent_id", parentId == null ? 0 : parentId);
        JSONObject root = post(PATH_PRODUCT_CATEGORY, param, false);
        return extractList(root, "category_list");
    }

    /**
     * 商品转链
     *
     * @param productUrl   商品链接（与商品列表返回的 detail_url 一致）
     * @param productExt   商品列表返回的 ext 字段，尽量传，可提高转链成功率
     * @param externalInfo 归因信息，只允许字母数字下划线，长度 ≤40
     * @param shareTypes   转链类型：1-deeplink 2-二维码 3-口令 4-zlink 5-sharelink；
     *                     不传默认只有 1。二维码生成较慢，不需要就别传 2
     * @return data 对象：dy_deeplink / dy_password / dy_qr_code / dy_zlink / dy_sharelink / coupon_link
     */
    public JSONObject productLink(String productUrl, String productExt,
                                  String externalInfo, List<Integer> shareTypes) {
        if (productUrl == null || productUrl.trim().isEmpty()) {
            throw new MshopException("商品链接不能为空");
        }
        Map<String, Object> param = new HashMap<>(8);
        param.put("product_url", productUrl.trim());
        if (productExt != null && !productExt.trim().isEmpty()) {
            param.put("product_ext", productExt.trim());
        }
        if (externalInfo != null && !externalInfo.trim().isEmpty()) {
            param.put("external_info", externalInfo.trim());
        }
        // 默认同时拿 deeplink + 口令，覆盖 APP 内唤起与复制分享两种场景
        param.put("share_type", (shareTypes == null || shareTypes.isEmpty())
                ? java.util.Arrays.asList(1, 3) : shareTypes);
        JSONObject root = post(PATH_PRODUCT_LINK, param, false);
        Object data = root == null ? null : root.get("data");
        if (data instanceof JSONObject) {
            return (JSONObject) data;
        }
        throw new MshopException("转链失败，返回数据异常");
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /**
     * @param allowEmptyResult 是否允许 200001（商品无结果）作为正常返回
     * @return 完整响应体；allowEmptyResult 且命中 200001 时返回 null
     */
    private JSONObject post(String path, Map<String, Object> bizData, boolean allowEmptyResult) {
        if (!config.isReady()) {
            throw new MshopException("穿山甲电商联盟未配置");
        }
        Map<String, Object> body = CsjEcomSigner.envelope(
                config.getAppId(), config.getSecureKey(), bizData);
        String url = config.getBaseUrl() + path;
        String json = JSON.toJSONString(body);

        String resp;
        try {
            resp = HttpRequest.post(url)
                    .header("Content-Type", "application/json;charset=utf-8")
                    .body(json)
                    .timeout(15000)
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("穿山甲电商请求异常 path={} err={}", path, e.getMessage());
            throw new MshopException("穿山甲接口调用失败，请检查网络");
        }

        log.info("穿山甲电商 {} 响应: {}", path, abbreviate(resp, 600));
        if (resp == null || resp.trim().isEmpty()) {
            throw new MshopException("穿山甲返回为空");
        }

        JSONObject root;
        try {
            root = JSON.parseObject(resp);
        } catch (Exception e) {
            throw new MshopException("穿山甲返回格式异常");
        }
        if (root == null) {
            throw new MshopException("穿山甲返回为空");
        }

        Long code = root.getLong("code");
        if (code == null || code == 0L) {
            return root;
        }
        if (code == CODE_NO_RESULT) {
            if (allowEmptyResult) {
                return null;
            }
            return root;
        }
        String desc = root.getString("desc");
        throw new MshopException(describeError(code, desc));
    }

    /**
     * 把官方错误码翻译成能直接给运营看的提示，原始 desc 附在后面便于排查
     */
    private String describeError(long code, String desc) {
        String base;
        switch ((int) code) {
            case 100002:
                base = "请求参数不合法";
                break;
            case 100003:
                base = "签名类型错误（只支持 MD5）";
                break;
            case 100004:
                base = "签名校验失败，请检查 secureKey 与 role_id";
                break;
            case 100005:
                base = "请求时间与服务器相差超过 10 分钟，请校准服务器时间";
                break;
            case 100006:
                base = "AppID 不合法，该应用可能未接入穿山甲电商联盟";
                break;
            case 100007:
                base = "接口禁止访问";
                break;
            case 100011:
                base = "暂无业务权限，请先在穿山甲后台完成流量备案";
                break;
            case 200001:
                base = "未搜索到结果";
                break;
            case 500000:
                base = "穿山甲服务端内部错误";
                break;
            default:
                base = "穿山甲接口调用失败";
        }
        return desc == null || desc.trim().isEmpty()
                ? base + "[" + code + "]"
                : base + "[" + code + "]：" + desc;
    }

    /**
     * data 可能是数组，也可能是 {list:[...]} 包一层，两种都兼容
     */
    private JSONArray extractList(JSONObject root, String... keys) {
        if (root == null) {
            return new JSONArray();
        }
        Object data = root.get("data");
        if (data instanceof JSONArray) {
            return (JSONArray) data;
        }
        if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            for (String key : keys) {
                JSONArray arr = obj.getJSONArray(key);
                if (arr != null) {
                    return arr;
                }
            }
        }
        return new JSONArray();
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
