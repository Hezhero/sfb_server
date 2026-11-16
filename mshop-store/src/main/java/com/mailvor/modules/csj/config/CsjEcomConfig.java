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
 * 穿山甲电商联盟（抖音电商 CPS）配置
 *
 * <p>与 {@link CsjConfig}（激励视频广告）是两个独立业务，互不影响。</p>
 *
 * <p>注意：商品搜索与转链接口需要先在穿山甲后台完成流量备案，
 * 否则调用会返回 {"code":100011,"desc":"暂无业务权限"}。
 * 订单查询接口的门槛较低，实名 + 角色绑定后即可调用。</p>
 *
 * @author mailvor
 */
@Slf4j
@Data
@Component
public class CsjEcomConfig {

    /** 总开关 */
    @Value("${csj-ecom.enabled:false}")
    private boolean enabled;

    /** 穿山甲媒体/应用 AppID */
    @Value("${csj-ecom.appId:}")
    private String appId;

    /**
     * 安全密钥。在穿山甲后台获取，对应 role_id 需与主账户 id 一致。
     * 只用于服务端签名，绝不能下发给客户端。
     */
    @Value("${csj-ecom.secureKey:}")
    private String secureKey;

    /** 默认推广位 ID（转链时使用，可空） */
    @Value("${csj-ecom.adzoneId:}")
    private String adzoneId;

    /** 网关地址 */
    @Value("${csj-ecom.baseUrl:https://ecom.pangolin-sdk-toutiao.com}")
    private String baseUrl;

    /** 分页大小，商品搜索接口上限为 20 */
    @Value("${csj-ecom.pageSize:20}")
    private int pageSize;

    public boolean isReady() {
        return enabled
                && appId != null && !appId.trim().isEmpty()
                && secureKey != null && !secureKey.trim().isEmpty();
    }

    /**
     * 启动自检：把配置状态打到日志里，避免"配了但没生效"这种问题靠猜。
     * secureKey 只输出长度与首尾各 4 位，不打印明文。
     */
    @PostConstruct
    public void logStatus() {
        if (!enabled) {
            log.info("穿山甲电商联盟：未启用（csj-ecom.enabled=false），相关接口会返回未配置");
            return;
        }
        String keyDesc;
        if (secureKey == null || secureKey.trim().isEmpty()) {
            keyDesc = "未配置";
        } else {
            String k = secureKey.trim();
            keyDesc = k.length() <= 8
                    ? "长度异常(" + k.length() + ")"
                    : k.substring(0, 4) + "****" + k.substring(k.length() - 4) + " (长度 " + k.length() + ")";
        }
        log.info("穿山甲电商联盟：enabled=true, appId={}, secureKey={}, pageSize={}",
                appId == null || appId.trim().isEmpty() ? "未配置" : appId,
                keyDesc, pageSize);
        if (!isReady()) {
            log.warn("穿山甲电商联盟：配置不完整，商品/订单接口不可用（需要 appId 与 secureKey 同时存在）");
        }
    }
}
