/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.csj.service;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 穿山甲电商联盟接口签名
 *
 * <p>签名规则（官方 28733 协议）：</p>
 * <pre>
 * signStr = "app_id=" + appId + "&data=" + data + "&req_id=" + reqId
 *         + "&timestamp=" + timestamp + secureKey
 * sign    = MD5(signStr)   // 32 位小写
 * </pre>
 *
 * <p><b>关键坑</b>：version、sign_type、sign 自身<b>不参与</b>加签。
 * 把它们拼进签名串会导致上游返回 {@code 100004 sign check fail}。</p>
 *
 * @author junchen
 */
public final class CsjEcomSigner {

    private CsjEcomSigner() {
    }

    /**
     * 计算签名
     *
     * @param appId     应用 ID
     * @param data      业务报文（紧凑 JSON 字符串，必须与最终发送的一致）
     * @param reqId     请求唯一标识
     * @param timestamp Unix 时间戳（秒）
     * @param secureKey 安全密钥
     * @return 32 位小写 MD5
     */
    public static String sign(String appId, String data, String reqId, long timestamp, String secureKey) {
        String signStr = "app_id=" + appId
                + "&data=" + data
                + "&req_id=" + reqId
                + "&timestamp=" + timestamp
                + secureKey;
        return DigestUtil.md5Hex(signStr, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 构造完整请求封包（含 sign）
     *
     * @param appId     应用 ID
     * @param secureKey 安全密钥
     * @param bizData   业务参数对象，内部会序列化为紧凑 JSON
     */
    public static Map<String, Object> envelope(String appId, String secureKey, Object bizData) {
        String data = JSON.toJSONString(bizData);
        // 官方示例使用带横线的标准 UUID；req_id 只用于追踪与幂等，不参与格式校验
        String reqId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> body = new LinkedHashMap<>(8);
        body.put("app_id", appId);
        body.put("data", data);
        body.put("req_id", reqId);
        body.put("sign_type", "MD5");
        body.put("timestamp", timestamp);
        body.put("version", "1");
        body.put("sign", sign(appId, data, reqId, timestamp, secureKey));
        return body;
    }
}
