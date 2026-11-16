package com.mailvor.modules.csj.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mailvor.api.MshopException;
import com.mailvor.enums.BillDetailEnum;
import com.mailvor.modules.csj.config.CsjConfig;
import com.mailvor.modules.csj.domain.MwCsjRewardLog;
import com.mailvor.modules.csj.service.MwCsjRewardService;
import com.mailvor.modules.csj.service.mapper.MwCsjRewardLogMapper;
import com.mailvor.modules.user.domain.MwUser;
import com.mailvor.modules.user.service.MwUserBillService;
import com.mailvor.modules.user.service.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 穿山甲激励视频奖励服务实现
 *
 * <p>防刷策略（三层）：</p>
 * <ol>
 *   <li>transaction_id 唯一索引 —— 同一笔奖励只能发一次，天然防重放</li>
 *   <li>单用户每日次数上限 —— 限制单账号收益</li>
 *   <li>两次奖励最小间隔 —— 挡住脚本高频调用</li>
 * </ol>
 *
 * <p><b>已知局限</b>：客户端上报"看完了"这件事本身无法在服务端独立证明。
 * 上面三层只能防重放和刷量，防不住被 hook 的客户端伪造 transactionId。
 * 要彻底解决，需要接穿山甲的服务端奖励回调（S2S），由字节服务器回调本站验签后发奖。</p>
 *
 * @author mailvor
 */
@Slf4j
@Service
@AllArgsConstructor
public class MwCsjRewardServiceImpl implements MwCsjRewardService {

    private static final int STATUS_REWARDED = 1;

    private final CsjConfig csjConfig;
    private final MwCsjRewardLogMapper rewardLogMapper;
    private final UserMapper userMapper;
    private final MwUserBillService billService;

    @Override
    public Map<String, Object> getClientConfig() {
        Map<String, Object> map = new HashMap<>(8);
        map.put("enabled", csjConfig.isReady());
        if (!csjConfig.isReady()) {
            return map;
        }
        map.put("appId", csjConfig.getAppId());
        map.put("rewardedVideoId", csjConfig.getRewardedVideoId());
        String iosId = csjConfig.getRewardedVideoIdIos();
        map.put("rewardedVideoIdIos",
                (iosId == null || iosId.trim().isEmpty()) ? csjConfig.getRewardedVideoId() : iosId);
        map.put("rewardIntegral", csjConfig.getRewardIntegral());
        map.put("dailyLimit", csjConfig.getDailyLimit());
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reward(Long uid, String transactionId, String posId, String ip) {
        if (!csjConfig.isReady()) {
            throw new MshopException("广告功能未开启");
        }
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new MshopException("参数错误");
        }
        String txId = transactionId.trim();
        if (txId.length() > 128) {
            throw new MshopException("参数错误");
        }

        // 幂等：同一流水号重复提交，直接返回已发放结果，不重复加积分
        MwCsjRewardLog existed = rewardLogMapper.selectOne(
                Wrappers.<MwCsjRewardLog>lambdaQuery()
                        .eq(MwCsjRewardLog::getTransactionId, txId)
                        .last("limit 1"));
        if (existed != null) {
            if (!existed.getUid().equals(uid)) {
                // 流水号被别人用过，说明客户端在伪造，直接拒绝
                throw new MshopException("奖励流水号无效");
            }
            log.info("穿山甲奖励重复提交 uid={} tx={}", uid, txId);
            return result(uid, 0, existed.getIntegral(), todayCount(uid), false);
        }

        MwUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new MshopException("用户不存在");
        }

        int dailyLimit = csjConfig.getDailyLimit();
        long todayCount = todayCount(uid);
        if (dailyLimit > 0 && todayCount >= dailyLimit) {
            throw new MshopException("今日次数已用完");
        }

        if (csjConfig.getMinIntervalSeconds() > 0) {
            MwCsjRewardLog last = rewardLogMapper.selectOne(
                    Wrappers.<MwCsjRewardLog>lambdaQuery()
                            .eq(MwCsjRewardLog::getUid, uid)
                            .eq(MwCsjRewardLog::getStatus, STATUS_REWARDED)
                            .orderByDesc(MwCsjRewardLog::getId)
                            .last("limit 1"));
            if (last != null && last.getCreateTime() != null) {
                long elapsed = (System.currentTimeMillis() - last.getCreateTime().getTime()) / 1000;
                if (elapsed < csjConfig.getMinIntervalSeconds()) {
                    throw new MshopException("操作过于频繁，请稍后再试");
                }
            }
        }

        int integral = csjConfig.getRewardIntegral();
        if (integral <= 0) {
            throw new MshopException("奖励配置有误");
        }

        MwCsjRewardLog record = MwCsjRewardLog.builder()
                .uid(uid)
                .transactionId(txId)
                .posId(posId)
                .integral(integral)
                .status(STATUS_REWARDED)
                .remark("激励视频奖励")
                .ip(ip)
                .build();
        try {
            rewardLogMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复提交，唯一索引兜底
            log.warn("穿山甲奖励并发重复 uid={} tx={}", uid, txId);
            throw new MshopException("奖励已发放");
        }

        userMapper.incIntegral(integral, uid);

        MwUser latest = userMapper.selectById(uid);
        double balance = latest == null || latest.getIntegral() == null
                ? integral : latest.getIntegral().doubleValue();

        billService.income(uid, "看广告奖励", BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_9.getValue(), integral, balance, "穿山甲激励视频", txId);

        log.info("穿山甲奖励发放 uid={} tx={} integral={} balance={}", uid, txId, integral, balance);
        return result(uid, integral, integral, todayCount + 1, true);
    }

    private long todayCount(Long uid) {
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Long count = rewardLogMapper.selectCount(
                Wrappers.<MwCsjRewardLog>lambdaQuery()
                        .eq(MwCsjRewardLog::getUid, uid)
                        .eq(MwCsjRewardLog::getStatus, STATUS_REWARDED)
                        .ge(MwCsjRewardLog::getCreateTime, start));
        return count == null ? 0L : count;
    }

    private Map<String, Object> result(Long uid, int granted, int integral,
                                       long todayCount, boolean success) {
        Map<String, Object> map = new HashMap<>(8);
        map.put("success", success);
        map.put("granted", granted);
        map.put("integral", integral);
        map.put("todayCount", todayCount);
        map.put("dailyLimit", csjConfig.getDailyLimit());
        MwUser latest = userMapper.selectById(uid);
        if (latest != null && latest.getIntegral() != null) {
            map.put("balance", latest.getIntegral().doubleValue());
        }
        return map;
    }
}
