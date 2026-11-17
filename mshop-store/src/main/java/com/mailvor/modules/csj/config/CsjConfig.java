/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.csj.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 穿山甲（Pangle）激励视频广告配置
 *
 * <p>所有配置项都带默认值，yml 不配也不会导致启动失败。
 * 真正启用需要：csj.enabled=true 且 appId / rewardedVideoId 都已填写。</p>
 *
 * <p><b>上线只需填 3 项配置（到 application-{dev,prod}.yml 的 csj 节点）：</b></p>
 * <ol>
 *   <li>csj.enabled=true —— 总开关（广告变现自助开通，无需商务审批）</li>
 *   <li>csj.appId —— 穿山甲后台创建应用后获得的应用 ID</li>
 *   <li>csj.rewardedVideoId —— Android 激励视频代码位 ID；
 *       iOS 用 csj.rewardedVideoIdIos，不填则沿用 Android 的</li>
 * </ol>
 * <p>穿山甲激励视频属于「广告变现」业务线，注册 + 资质认证 + 创建代码位即可自助启用，
 * 与电商联盟（csj-ecom）的权限考核相互独立。</p>
 *
 * @author mailvor
 */
@Slf4j
@Data
@Component
public class CsjConfig {

    /** 总开关，false 时客户端不下发配置、拒绝发奖 */
    @Value("${csj.enabled:false}")
    private boolean enabled;

    /** 穿山甲应用 ID（平台后台创建应用后获得） */
    @Value("${csj.appId:}")
    private String appId;

    /** Android 激励视频代码位 ID */
    @Value("${csj.rewardedVideoId:}")
    private String rewardedVideoId;

    /** iOS 激励视频代码位 ID（不填则沿用 Android 的） */
    @Value("${csj.rewardedVideoIdIos:}")
    private String rewardedVideoIdIos;

    /** 完整观看一次发放的积分 */
    @Value("${csj.rewardIntegral:10}")
    private int rewardIntegral;

    /** 单用户每日最多可领次数 */
    @Value("${csj.dailyLimit:5}")
    private int dailyLimit;

    /** 两次奖励之间的最小间隔（秒），防止脚本刷接口 */
    @Value("${csj.minIntervalSeconds:30}")
    private int minIntervalSeconds;

    /**
     * 配置是否可用于对外下发。
     * 只有开关打开且必填项齐全才算可用，避免客户端拿到空 appId 后初始化崩溃。
     */
    public boolean isReady() {
        return enabled
                && appId != null && !appId.trim().isEmpty()
                && rewardedVideoId != null && !rewardedVideoId.trim().isEmpty();
    }

    /**
     * 启动自检：对齐 {@link CsjEcomConfig#logStatus()}，把配置状态打到日志里，
     * 避免"配了但没生效"靠猜。<b>appId 只输出首尾各 4 位，绝不打印明文。</b>
     */
    @PostConstruct
    public void logStatus() {
        if (!enabled) {
            log.info("穿山甲激励视频：未启用（csj.enabled=false），APP 不展示入口且发奖接口会拒绝");
            return;
        }
        log.info("穿山甲激励视频：enabled=true, appId={}, rewardedVideoId={}, rewardIntegral={}, dailyLimit={}",
                maskAppId(appId),
                rewardedVideoId == null || rewardedVideoId.trim().isEmpty() ? "未配置" : rewardedVideoId,
                rewardIntegral, dailyLimit);
        if (!isReady()) {
            log.warn("穿山甲激励视频：配置不完整，广告功能不可用（需要 appId 与 rewardedVideoId 同时存在）");
        }
    }

    /**
     * appId 脱敏：首尾各保留 4 位，中间用星号遮挡；空值/过短值不外泄原文。
     * 抽成静态纯函数便于单元测试验证"不明文输出"。
     */
    public static String maskAppId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未配置";
        }
        String v = value.trim();
        if (v.length() <= 8) {
            return "长度异常(" + v.length() + ")";
        }
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }
}
