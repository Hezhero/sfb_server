/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.csj.service;

import cn.hutool.crypto.digest.DigestUtil;

import java.nio.charset.StandardCharsets;

/**
 * 穿山甲激励视频「服务端奖励回调（S2S）」验签工具。
 *
 * <p>官方协议（supportcenter/5416）：</p>
 * <pre>
 * 穿山甲以 GET 请求开发者回调地址，回传：
 *   user_id / trans_id / reward_name / reward_amount / extra / sign
 *
 * 签名规则：sign = sha256(SecurityKey + ":" + trans_id)
 * 注意：只取 trans_id 参与加签，不涉及其它参数；冒号是字面分隔符。
 * 我方返回 JSON：{"isValid": true}
 * </pre>
 *
 * <p><b>与 {@link CsjEcomSigner} 的区别</b>：后者是电商联盟 CPS 接口的 MD5 签名；
 * 本类是广告变现 S2S 回调的 SHA-256 验签，两者使用不同凭证、不同算法，不可混用。</p>
 */
public final class CsjS2sSigner {

    private CsjS2sSigner() {
    }

    /**
     * 计算 S2S 回调签名：sha256(securityKey:transId)。
     *
     * @param securityKey 穿山甲广告变现的 app security key（不是电商联盟的 secureKey）
     * @param transId     回调回传的 trans_id
     * @return 32 字节 SHA-256 的十六进制小写串
     */
    public static String sign(String securityKey, String transId) {
        if (securityKey == null || transId == null) {
            throw new IllegalArgumentException("securityKey 与 transId 均不能为空");
        }
        String raw = securityKey + ":" + transId;
        return DigestUtil.sha256Hex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验回调签名是否正确。
     *
     * <p>使用常量时间比较（逐字节异或累积）而非 {@code String.equals}，
     * 避免通过响应时间差异逐字符推断正确签名（时序攻击）。</p>
     *
     * @param securityKey 服务端配置的 security key
     * @param transId     回调回传的 trans_id
     * @param sign        回调回传的 sign
     * @return 签名一致返回 true；任一参数缺失或不一致返回 false
     */
    public static boolean verify(String securityKey, String transId, String sign) {
        if (securityKey == null || securityKey.trim().isEmpty()
                || transId == null || transId.trim().isEmpty()
                || sign == null || sign.trim().isEmpty()) {
            return false;
        }
        String expected = sign(securityKey, transId);
        return constantTimeEquals(expected, sign.trim());
    }

    /**
     * 常量时间字符串比较。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            // 长度不同仍需做一次完整遍历，避免长度检查造成时序差异
            int diff = b.length();
            for (int i = 0; i < b.length(); i++) {
                diff |= b.charAt(i);
            }
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
