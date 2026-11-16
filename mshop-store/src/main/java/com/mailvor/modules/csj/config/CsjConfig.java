/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.csj.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 穿山甲（Pangle）激励视频广告配置
 *
 * <p>所有配置项都带默认值，yml 不配也不会导致启动失败。
 * 真正启用需要：csj.enabled=true 且 appId / rewardedVideoId 都已填写。</p>
 *
 * @author mailvor
 */
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
}
