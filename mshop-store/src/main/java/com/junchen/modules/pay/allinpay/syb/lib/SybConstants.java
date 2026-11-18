package com.junchen.modules.pay.allinpay.syb.lib;

/**
 * 通联支付 SDK 常量。
 *
 * <p><b>安全说明（2026-11-18）</b>：原文件保留了一大段被注释掉的「正式/测试环境调试参数」，
 * 其中包含真实的商户 SM2 私钥、RSA 私钥与 MD5 appKey。虽然处于注释状态、且全仓库零引用，
 * 但 git 历史审计将其判定为高危残留——注释不是删除，密钥一旦泄露即应视为失效。</p>
 *
 * <p>本类现在只保留空壳以兼容可能的历史 import。所有真实凭据必须：
 * <ol>
 *   <li>从服务端配置 / 环境变量注入，不得写入源码；</li>
 *   <li>作为已泄露密钥立即在通联商户后台轮换。</li>
 * </ol>
 * 详见 docs/14-Git历史敏感信息审计.md。</p>
 */
public final class SybConstants {

    private SybConstants() {
    }
}
