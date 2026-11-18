package com.junchen.modules.csj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.junchen.api.MshopException;
import com.junchen.modules.csj.domain.CsjS2sWhitelist;
import com.junchen.modules.csj.service.mapper.CsjS2sWhitelistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 穿山甲 S2S 服务端奖励回调服务。
 *
 * <p>两层校验的核心第一层：验签通过后把 trans_id 落库为「可发放」。
 * 客户端上报奖励时由 {@code MwCsjRewardServiceImpl} 检查本服务的白名单。</p>
 */
@Service
@Slf4j
public class CsjS2sService {

    /** 广告变现的 app security key（与电商联盟的 secureKey 不同） */
    @Value("${csj.s2s-security-key:}")
    private String securityKey;

    @Resource
    private CsjS2sWhitelistMapper whitelistMapper;

    /**
     * S2S 是否已启用（配置了 security key）。发奖服务据此决定是否强制白名单校验。
     */
    public boolean isEnabled() {
        return securityKey != null && !securityKey.trim().isEmpty();
    }

    /**
     * 处理穿山甲 S2S 回调。
     *
     * @return 验签通过且落库成功返回 true；否则 false（我方需返回 isValid=false）
     */
    public boolean handleCallback(String transId, String uid, String rewardName,
                                  Integer rewardAmount, String extra, String sign, String sourceIp) {
        if (transId == null || transId.trim().isEmpty()) {
            log.warn("S2S 回调缺少 trans_id，来源IP={}", sourceIp);
            return false;
        }
        String tx = transId.trim();
        if (!CsjS2sSigner.verify(securityKey, tx, sign)) {
            log.warn("S2S 回调验签失败 transId={} 来源IP={}", tx, sourceIp);
            return false;
        }

        // 幂等：同一 trans_id 重复回调不报错（上游会重试），直接确认
        CsjS2sWhitelist existed = whitelistMapper.selectOne(
                new LambdaQueryWrapper<CsjS2sWhitelist>()
                        .eq(CsjS2sWhitelist::getTransId, tx)
                        .last("limit 1"));
        if (existed != null) {
            log.info("S2S 回调重复，transId={}", tx);
            return true;
        }

        Long uidLong = parseUid(uid);
        CsjS2sWhitelist entry = CsjS2sWhitelist.builder()
                .transId(tx)
                .uid(uidLong)
                .rewardName(rewardName)
                .rewardAmount(rewardAmount)
                .extra(extra)
                .consumed(CsjS2sWhitelist.CONSUMED_NO)
                .sourceIp(sourceIp)
                .build();
        entry.setCreateTime(new Date());
        entry.setUpdateTime(new Date());
        try {
            whitelistMapper.insert(entry);
            log.info("S2S 回调验签通过并落库 transId={} uid={}", tx, uidLong);
            return true;
        } catch (Exception e) {
            // 唯一索引冲突 = 并发重复回调，视为成功
            log.warn("S2S 白名单插入冲突（并发重试）transId={} err={}", tx, e.getMessage());
            return true;
        }
    }

    /**
     * 校验 trans_id 是否在白名单且未被消费。
     */
    public boolean isEligible(String transId, Long uid) {
        if (transId == null || uid == null) {
            return false;
        }
        CsjS2sWhitelist entry = whitelistMapper.selectOne(
                new LambdaQueryWrapper<CsjS2sWhitelist>()
                        .eq(CsjS2sWhitelist::getTransId, transId.trim())
                        .last("limit 1"));
        if (entry == null) {
            return false;
        }
        // 白名单记录的 uid 若非空，必须与上报用户一致（防止 trans_id 被他人冒用）
        if (entry.getUid() != null && !entry.getUid().equals(uid)) {
            return false;
        }
        return entry.getConsumed() == null || entry.getConsumed() == CsjS2sWhitelist.CONSUMED_NO;
    }

    /**
     * 标记 trans_id 已消费（发奖成功后调用）。
     */
    public void markConsumed(String transId) {
        if (transId == null) {
            return;
        }
        CsjS2sWhitelist entry = whitelistMapper.selectOne(
                new LambdaQueryWrapper<CsjS2sWhitelist>()
                        .eq(CsjS2sWhitelist::getTransId, transId.trim())
                        .last("limit 1"));
        if (entry == null) {
            return;
        }
        entry.setConsumed(CsjS2sWhitelist.CONSUMED_YES);
        entry.setUpdateTime(new Date());
        whitelistMapper.updateById(entry);
    }

    private Long parseUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(uid.trim());
        } catch (NumberFormatException e) {
            log.warn("S2S 回调 user_id 非数字：{}", uid);
            return null;
        }
    }
}
