/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.api.MshopException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 好单库开放平台 v3 统一接口（依据官方 AI 文档包 haodanku-openapi-docs）
 *
 * <p>与 {@link KuService} 的关系：KuService 是历史实现，混用了 v2、v3、CMS 与增值类接口；
 * 本类只承载官方文档里定义的 v3 统一接口，不改动原有代码。</p>
 *
 * <p><b>鉴权</b>：普通接口统一使用 {@code apikey}；增值类接口（/rest）需要
 * app_id + appsecret + sign，仍由 {@link KuService} 负责。</p>
 *
 * <p><b>成功码</b>：官方明确 {@code code == 1} 或 {@code code == 200} 都是成功，
 * 其余一律失败。历史代码只判断了 200，本类统一处理。</p>
 *
 * <p><b>用法</b>：所有接口都可以通过 {@link #get(String, Map)} /
 * {@link #postForm(String, Map)} 调用；高频场景提供了语义化便捷方法。</p>
 *
 * @author junchen
 */
@Slf4j
@Component
public class KuOpenApiService {

    private static final String BASE = "https://v3.api.haodanku.com/";

    @Value("${haodanku.key:}")
    private String apikey;

    // ------------------------------------------------------------------
    // 接口路径常量（官方文档包 interfaces/ 6 个分类）
    // ------------------------------------------------------------------

    /** 商品接口 */
    public static final String P_COLUMN = "column";                              // 淘宝商品列表
    public static final String P_ITEM_DETAIL = "item_detail";                    // 淘宝单品详情
    public static final String P_SUPERSEARCH = "supersearch";                    // 淘宝商品搜索
    public static final String P_TBLIVE_GOODS_QUERY = "tblive_goods_query";       // 淘宝更高佣查询
    public static final String P_UNIFY_JDGOODS_SEARCH = "unify_jdgoods_search";   // 京东搜索/详情/列表
    public static final String P_UNIFY_PDD_ITEMLIST = "unify_pdd_itemlist";       // 拼多多列表
    public static final String P_UNIFY_PDD_GOODS_SEARCH = "unify_pdd_goods_search"; // 拼多多搜索/详情
    public static final String P_UNIFY_VIP_ITEM_LIST = "unify_vip_item_list";     // 唯品会列表
    public static final String P_UNIFY_VIP_ITEM_QUERY = "unify_vip_item_query";   // 唯品会搜索/详情
    public static final String P_DY_ITEMLIST = "dy_itemlist_simplify";            // 抖音列表/搜索
    public static final String P_DY_ITEM_INFO = "dy_item_info";                    // 抖音详情
    public static final String P_KS_ITEM_LIST = "ks_item_list";                    // 快手列表/搜索
    public static final String P_KS_ITEM_DETAIL = "ks_item_detail";                // 快手详情
    public static final String P_IDLE_GOODS_QUERY = "idle_goods_query";            // 闲鱼列表
    public static final String P_IDLE_GOODS_DETAIL = "idle_goods_detail";          // 闲鱼详情
    public static final String P_XY_PREPAID_RECHARGE = "xy_prepaid_recharge";      // 闲鱼话费充值

    /** 转链接口 */
    public static final String P_RATESURL = "ratesurl";                            // 淘宝商品转链
    public static final String P_GET_TBLIVE_LINK = "get_tblive_link";              // 淘宝更高佣转链
    public static final String P_UNIFY_JDITEMS_LINK = "unify_jditems_link";        // 京东转链
    public static final String P_UNIFY_PDDITEMS_LINK = "unify_pdditems_link";      // 拼多多转链
    public static final String P_UNIFY_VIP_ITEM_CONVERT = "unify_vip_item_convert"; // 唯品会转链
    public static final String P_GET_DYITEM_LINK = "get_dyitem_link";              // 抖音转链
    public static final String P_KS_ITEM_RATESURL = "ks_item_ratesurl";            // 快手转链
    public static final String P_IDLE_GENERAL_LINK = "idle_general_link";          // 闲鱼转链

    /** 订单接口（订单权限通常需单独申请） */
    public static final String P_UNIFY_JD_ORDER_LIST = "unify_jd_order_list";
    public static final String P_UNIFY_PDD_ORDER_LIST = "unify_pdd_order_list";
    public static final String P_DY_ORDER_LIST = "dy_order_list";
    public static final String P_VIP_UNION_ORDER_LIST = "vip_union_order_list";
    public static final String P_KS_ORDER_LIST = "ks_order_list";
    public static final String P_ELM_ORDER_LIST = "elm_order_list";
    public static final String P_MT_ORDER_LIST = "mt_order_list";
    public static final String P_TC_ORDER_LIST = "tc_order_list";
    public static final String P_HV_ORDER_LIST = "hv_order_list";
    public static final String P_TB_LIVE_ORDER_LIST = "tb_live_order_list";
    public static final String P_IDLE_ORDER_LIST = "idle_order_list";

    /** 本地生活 / 活动 / 口令工具 */
    public static final String P_MEITUAN_ACTIVITY_LIST = "meituan_activity_list";
    public static final String P_MEITUAN_RATESURL = "meituan_ratesurl";
    public static final String P_ELM_ACTIVITY_LIST = "elm_activity_list";
    public static final String P_ELM_ACTIVITY_RATESURL = "elm_activity_ratesurl";
    public static final String P_TCHOTEL_RATESURL = "tchotel_ratesurl";
    public static final String P_HV_RATESURL = "hv_ratesurl";
    public static final String P_MT_GOODS_DETAIL = "mt_goods_detail";
    public static final String P_UNIFY_PDDPROMURL_GENERATE = "unify_pddpromurl_generate";
    public static final String P_UNIFY_VIP_ACTIVITY_CONVERT = "unify_vip_activity_convert";
    public static final String P_GET_DYACTIVITY_LINK = "get_dyactivity_link";
    public static final String P_DY_ANALYZE_CODE = "dy_analyze_code";              // 抖音口令解析
    public static final String P_KS_ANALYZE_CODE = "ks_analyze_code";              // 快手口令解析

    // ------------------------------------------------------------------
    // 通用调用入口
    // ------------------------------------------------------------------

    /**
     * GET 调用普通接口（自动注入 apikey）
     */
    public JSONObject get(String path, Map<String, Object> params) {
        return execute(path, params, false);
    }

    /**
     * POST form-data 调用普通接口（自动注入 apikey）
     */
    public JSONObject postForm(String path, Map<String, Object> params) {
        return execute(path, params, true);
    }

    private JSONObject execute(String path, Map<String, Object> params, boolean post) {
        if (apikey == null || apikey.trim().isEmpty()) {
            throw new MshopException("好单库 apikey 未配置");
        }
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("apikey", apikey.trim());
        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null) {
                    all.put(k, v);
                }
            });
        }
        String url = BASE + path;
        try {
            String resp = post ? HttpUtil.post(url, all) : HttpUtil.get(url, all);
            log.info("好单库 {} {} 参数={} 响应={}", post ? "POST" : "GET", path,
                    mask(all), abbreviate(resp, 500));
            if (resp == null || resp.trim().isEmpty()) {
                return error(-1, "好单库返回为空");
            }
            return JSON.parseObject(resp);
        } catch (Exception e) {
            log.error("好单库接口异常 path={} err={}", path, e.getMessage());
            return error(-1, "好单库接口调用失败：" + e.getMessage());
        }
    }

    /** 官方约定：code 为 1 或 200 才算成功 */
    public static boolean isSuccess(JSONObject res) {
        if (res == null) {
            return false;
        }
        Integer code = res.getInteger("code");
        return code != null && (code == 1 || code == 200);
    }

    public static String errorMsg(JSONObject res) {
        return res == null ? "响应为空" : res.getString("msg");
    }

    // ------------------------------------------------------------------
    // 高频便捷方法
    // ------------------------------------------------------------------

    /** 快手商品列表/搜索（普通接口，无需额外权限） */
    public JSONObject ksItemList(String keyword, Integer minId, Integer back, Integer sortType) {
        return get(P_KS_ITEM_LIST, params(
                "keyword", keyword, "min_id", minId, "back", back, "sort_type", sortType));
    }

    /** 快手商品详情 */
    public JSONObject ksItemDetail(String productId) {
        return get(P_KS_ITEM_DETAIL, params("product_id", productId));
    }

    /** 快手商品转链，返回 linkCode / kwaiUrl / linkUrl */
    public JSONObject ksItemRatesurl(String productId, String channel) {
        return postForm(P_KS_ITEM_RATESURL, params("product_id", productId, "channel", channel));
    }

    /** 快手口令解析 */
    public JSONObject ksAnalyzeCode(String content) {
        return postForm(P_KS_ANALYZE_CODE, params("content", content));
    }

    /** 抖音口令解析 */
    public JSONObject dyAnalyzeCode(String content, Integer isChange) {
        return postForm(P_DY_ANALYZE_CODE, params("content", content, "is_change", isChange));
    }

    /** 闲鱼商品列表 */
    public JSONObject idleGoodsQuery(String keyword, Integer minId) {
        return get(P_IDLE_GOODS_QUERY, params("keyword", keyword, "min_id", minId));
    }

    /** 闲鱼商品详情（加密ID与数字ID二选一） */
    public JSONObject idleGoodsDetail(String itemId, String plainItemId) {
        return get(P_IDLE_GOODS_DETAIL, params("itemid", itemId, "plain_itemid", plainItemId));
    }

    /** 闲鱼推广转链（商品/口令/活动通用） */
    public JSONObject idleGeneralLink(Integer materialType, String itemId,
                                      String xyUrl, Integer activityPageId, String channel) {
        return postForm(P_IDLE_GENERAL_LINK, params(
                "material_type", materialType, "itemid", itemId, "xy_url", xyUrl,
                "activity_page_id", activityPageId, "channel", channel));
    }

    /**
     * 订单拉取通用方法。
     *
     * @param path      P_XXX_ORDER_LIST 常量之一
     * @param startDate 开始时间，Unix 秒
     * @param endDate   结束时间，Unix 秒
     */
    public JSONObject orderList(String path, long startDate, long endDate,
                                Integer minId, Integer back, Map<String, Object> extra) {
        Map<String, Object> p = params(
                "min_id", minId == null ? 1 : minId,
                "back", back == null ? 100 : Math.min(back, 100),
                "start_date", startDate, "end_date", endDate);
        if (extra != null) {
            p.putAll(extra);
        }
        return get(path, p);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 可变参数构造请求参数，自动跳过 null 值 */
    public static Map<String, Object> params(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                m.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return m;
    }

    private JSONObject error(int code, String msg) {
        JSONObject o = new JSONObject();
        o.put("code", code);
        o.put("msg", msg);
        return o;
    }

    /** 日志里不打印 apikey */
    private String mask(Map<String, Object> all) {
        Map<String, Object> copy = new LinkedHashMap<>(all);
        copy.put("apikey", "***");
        return copy.toString();
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
