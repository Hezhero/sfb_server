package com.junchen.csj;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.junchen.api.MshopException;
import com.junchen.modules.csj.config.CsjConfig;
import com.junchen.modules.csj.domain.MwCsjRewardLog;
import com.junchen.modules.csj.service.impl.MwCsjRewardServiceImpl;
import com.junchen.modules.csj.service.mapper.MwCsjRewardLogMapper;
import com.junchen.modules.user.domain.MwUser;
import com.junchen.modules.user.service.MwUserBillService;
import com.junchen.modules.user.service.mapper.UserMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 激励视频发奖服务契约测试（工单 09）。
 *
 * <p>接缝：{@link MwCsjRewardServiceImpl#reward(Long, String, String, String)} 的公开行为。
 * Mapper / 账单服务全部 mock，不连真实数据库。覆盖工单要求：开关关闭拒绝、
 * 正常发奖、同一 transactionId 重复提交不重复发放。</p>
 */
public class MwCsjRewardServiceTest {

    private static final Long UID = 42L;
    private static final String TX = "csj_1000_abcd";
    private static final int INTEGRAL = 10;

    private CsjConfig config;
    private MwCsjRewardLogMapper rewardLogMapper;
    private UserMapper userMapper;
    private MwUserBillService billService;
    private MwCsjRewardServiceImpl service;

    @Before
    public void setUp() {
        config = new CsjConfig();
        config.setEnabled(true);
        config.setAppId("5000001");
        config.setRewardedVideoId("102038");
        config.setRewardIntegral(INTEGRAL);
        config.setDailyLimit(5);
        config.setMinIntervalSeconds(0); // 关闭间隔限制，专注幂等断言

        rewardLogMapper = mock(MwCsjRewardLogMapper.class);
        userMapper = mock(UserMapper.class);
        billService = mock(MwUserBillService.class);

        // 用户查得到、初始余额 100；每次查询都返回该用户
        MwUser user = new MwUser();
        user.setIntegral(new BigDecimal("100"));
        when(userMapper.selectById(UID)).thenReturn(user);

        // 默认无历史记录、今日次数 0
        when(rewardLogMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(rewardLogMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service = new MwCsjRewardServiceImpl(config, rewardLogMapper, userMapper, billService);
    }

    @Test
    public void rewardIsRejectedWhenSwitchIsOff() {
        config.setEnabled(false);
        try {
            service.reward(UID, TX, "102038", "127.0.0.1");
            fail("开关关闭时应拒绝发奖");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("未开启"));
        }
        verify(rewardLogMapper, never()).insert(any(MwCsjRewardLog.class));
        verify(userMapper, never()).incIntegral(anyDouble(), anyLong());
        verify(billService, never()).income(anyLong(), anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyString(), anyString());
    }

    @Test
    public void firstSubmissionGrantsIntegralAndWritesLog() {
        Map<String, Object> result = service.reward(UID, TX, "102038", "127.0.0.1");

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(INTEGRAL, result.get("granted"));
        assertEquals(INTEGRAL, result.get("integral"));

        ArgumentCaptor<MwCsjRewardLog> logCaptor = ArgumentCaptor.forClass(MwCsjRewardLog.class);
        verify(rewardLogMapper, times(1)).insert(logCaptor.capture());
        assertEquals(TX, logCaptor.getValue().getTransactionId());
        assertEquals(UID, logCaptor.getValue().getUid());
        assertEquals(Integer.valueOf(INTEGRAL), logCaptor.getValue().getIntegral());

        verify(userMapper, times(1)).incIntegral(INTEGRAL, UID);
        verify(billService, times(1)).income(anyLong(), anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyString(), anyString());
    }

    @Test
    public void duplicateTransactionIdDoesNotGrantIntegralTwice() {
        // 第一次发放成功，落一条已发放记录
        service.reward(UID, TX, "102038", "127.0.0.1");

        MwCsjRewardLog existing = MwCsjRewardLog.builder()
                .uid(UID)
                .transactionId(TX)
                .integral(INTEGRAL)
                .status(1)
                .build();
        // 第二次查询命中同一流水号历史记录 → 走幂等分支
        when(rewardLogMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        Map<String, Object> second = service.reward(UID, TX, "102038", "127.0.0.1");

        assertEquals("重复提交应返回未新发放", Boolean.FALSE, second.get("success"));
        assertEquals("重复提交不应再次加分", 0, second.get("granted"));
        assertEquals("仍返回本次奖励积分值用于展示", INTEGRAL, second.get("integral"));

        // 关键断言：只有第一次插入/加分/记账，第二次全部跳过
        verify(rewardLogMapper, times(1)).insert(any(MwCsjRewardLog.class));
        verify(userMapper, times(1)).incIntegral(anyDouble(), eq(UID));
        verify(billService, times(1)).income(anyLong(), anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyString(), anyString());
    }

    @Test
    public void concurrentDuplicateKeyIsToleratedAndDoesNotGrantIntegral() {
        // 并发场景：insert 时唯一索引冲突抛 DuplicateKeyException
        when(rewardLogMapper.insert(any(MwCsjRewardLog.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate key"));
        try {
            service.reward(UID, TX, "102038", "127.0.0.1");
            fail("并发重复提交应抛出明确异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("已发放"));
        }
        verify(userMapper, never()).incIntegral(anyDouble(), anyLong());
        verify(billService, never()).income(anyLong(), anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyString(), anyString());
    }
}
