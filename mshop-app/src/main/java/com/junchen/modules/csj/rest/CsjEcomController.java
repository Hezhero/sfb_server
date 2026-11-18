/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.csj.rest;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.junchen.annotation.AnonymousAccess;
import com.junchen.api.ApiResult;
import com.junchen.api.MshopException;
import com.junchen.common.bean.LocalUser;
import com.junchen.common.interceptor.AuthCheck;
import com.junchen.modules.csj.service.CsjEcomClient;
import com.junchen.modules.user.domain.MwUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 穿山甲电商联盟（抖音电商 CPS）
 *
 * <p><b>前置条件</b>：商品列表 / 详情 / 类目 / 转链接口都需要在穿山甲后台完成流量备案，
 * 未备案会返回「暂无业务权限」。订单查询门槛较低，实名 + 角色绑定即可。</p>
 *
 * <p>字段名严格按官方 28733 协议：商品用 title / cover / cos_ratio / cos_fee，
 * 转链归因用 external_info，转链结果用 dy_deeplink / dy_password。</p>
 *
 * @author junchen
 */
@RestController
@Api(value = "穿山甲电商联盟", tags = "穿山甲：抖音电商CPS")
@RequestMapping("/csj/ecom")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CsjEcomController {

    private final CsjEcomClient csjEcomClient;

    /**
     * 商品列表（匿名可访问，不涉及用户数据）
     */
    @AnonymousAccess
    @GetMapping("/products")
    @ApiOperation(value = "抖音商品列表", notes = "需穿山甲后台完成流量备案，否则返回暂无业务权限")
    public ApiResult<Object> products(@RequestParam(required = false) String title,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(required = false) Integer pageSize) {
        JSONArray arr = csjEcomClient.productSearch(title, page, pageSize);
        List<Map<String, Object>> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            list.add(productVo(arr.getJSONObject(i)));
        }
        int size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 20);
        Map<String, Object> result = new HashMap<>(4);
        result.put("list", list);
        result.put("page", page);
        result.put("hasMore", arr.size() >= size);
        return ApiResult.ok(result);
    }

    /**
     * 商品详情（匿名）
     */
    @AnonymousAccess
    @GetMapping("/products/detail")
    @ApiOperation(value = "抖音商品详情", notes = "ids 用英文逗号分隔")
    public ApiResult<Object> productDetail(@RequestParam String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            throw new MshopException("商品 ID 不能为空");
        }
        List<Long> productIds = new ArrayList<>();
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                try {
                    productIds.add(Long.parseLong(trimmed));
                } catch (NumberFormatException e) {
                    throw new MshopException("商品 ID 格式错误：" + trimmed);
                }
            }
        }
        JSONArray arr = csjEcomClient.productDetail(productIds);
        List<Map<String, Object>> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            Map<String, Object> vo = productVo(item);
            // 详情接口额外字段
            vo.put("imgs", item.get("imgs"));
            vo.put("inStock", item.get("in_stock"));
            vo.put("couponPrice", fen2yuan(item.get("coupon_price")));
            vo.put("commentScore", item.get("comment_score"));
            vo.put("commentNum", item.get("comment_num"));
            vo.put("orderNum", item.get("order_num"));
            list.add(vo);
        }
        return ApiResult.ok(list);
    }

    /**
     * 商品类目（匿名），parentId 传 0 查一级类目
     */
    @AnonymousAccess
    @GetMapping("/categories")
    @ApiOperation(value = "抖音商品类目", notes = "parentId=0 查一级，传一级类目 id 查二级")
    public ApiResult<Object> categories(@RequestParam(defaultValue = "0") Integer parentId) {
        JSONArray arr = csjEcomClient.productCategory(parentId);
        List<Map<String, Object>> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            Map<String, Object> vo = new HashMap<>(4);
            vo.put("id", item.get("id"));
            vo.put("name", item.get("name"));
            vo.put("level", item.get("level"));
            list.add(vo);
        }
        return ApiResult.ok(list);
    }

    /**
     * 商品转链（需登录，external_info 中带 uid 用于订单归因）
     */
    @AuthCheck
    @PostMapping("/link")
    @ApiOperation(value = "抖音商品转链", notes = "返回 deeplink、口令、二维码、分享链接")
    public ApiResult<Object> link(@RequestBody Map<String, String> param) {
        MwUser user = LocalUser.getUser();
        if (user == null) {
            throw new MshopException("请先登录");
        }
        String productUrl = param.get("productUrl");
        if (productUrl == null || productUrl.trim().isEmpty()) {
            throw new MshopException("商品链接不能为空");
        }
        String productExt = param.get("productExt");
        // external_info 只允许字母数字下划线，长度 ≤40；
        // 同时必须满足 OrderUtil.getDyOrderUser：按下划线切分后第二段是 uid
        String externalInfo = "csj_" + user.getUid() + "_" + (1000 + new Random().nextInt(9000));

        JSONObject data = csjEcomClient.productLink(productUrl, productExt, externalInfo, null);

        Map<String, Object> result = new HashMap<>(8);
        result.put("deepLink", data.get("dy_deeplink"));
        result.put("password", data.get("dy_password"));
        result.put("qrCode", data.get("dy_qr_code"));
        result.put("zlink", data.get("dy_zlink"));
        result.put("shareLink", data.get("dy_sharelink"));
        result.put("couponLink", data.get("coupon_link"));
        result.put("externalInfo", externalInfo);
        return ApiResult.ok(result);
    }

    /**
     * 商品对象转 APP 友好字段
     */
    private Map<String, Object> productVo(JSONObject item) {
        Map<String, Object> vo = new HashMap<>(16);
        vo.put("productId", item.get("product_id"));
        vo.put("title", item.get("title"));
        vo.put("cover", item.get("cover"));
        vo.put("price", fen2yuan(item.get("price")));
        vo.put("cosRatio", item.get("cos_ratio"));
        vo.put("cosFee", fen2yuan(item.get("cos_fee")));
        vo.put("sales", item.get("sales"));
        vo.put("detailUrl", item.get("detail_url"));
        vo.put("ext", item.get("ext"));
        vo.put("shopId", item.get("shop_id"));
        vo.put("shopName", item.get("shop_name"));
        vo.put("inStock", item.get("in_stock"));
        vo.put("couponPrice", fen2yuan(item.get("coupon_price")));
        return vo;
    }

    private Double fen2yuan(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value)) / 100.0d;
        } catch (Exception e) {
            return null;
        }
    }
}
