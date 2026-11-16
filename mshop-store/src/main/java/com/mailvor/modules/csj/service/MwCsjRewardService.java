package com.mailvor.modules.csj.service;

import java.util.Map;

/**
 * 穿山甲激励视频奖励服务
 *
 * @author mailvor
 */
public interface MwCsjRewardService {

    /**
     * 下发给客户端的广告配置（不含任何密钥）
     */
    Map<String, Object> getClientConfig();

    /**
     * 发放激励视频奖励
     *
     * @param uid           用户 uid
     * @param transactionId 穿山甲奖励流水号，用于幂等
     * @param posId         广告位 ID
     * @param ip            请求 IP
     * @return 本次发放结果与最新余额
     */
    Map<String, Object> reward(Long uid, String transactionId, String posId, String ip);
}
