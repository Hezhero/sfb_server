package com.mailvor.dataoke;

import com.mailvor.api.MshopException;
import com.mailvor.modules.tk.config.TbConfig;
import com.mailvor.modules.tk.domain.MwTbFiling;
import com.mailvor.modules.tk.domain.MwTbFilingLog;
import com.mailvor.modules.tk.service.DataokeService;
import com.mailvor.modules.tk.service.TbMemberFilingService;
import com.mailvor.modules.tk.service.mapper.MwTbFilingLogMapper;
import com.mailvor.modules.tk.service.mapper.MwTbFilingMapper;
import com.mailvor.modules.tk.vo.TbFilingResultVo;
import com.mailvor.modules.tk.vo.TbPublisherInfoVo;
import com.mailvor.modules.tk.vo.TbRelationRecordVo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 会员备案服务契约测试（工单 03）。
 *
 * <p>接缝：{@link TbMemberFilingService} 的公开方法。上游 HTTP 由
 * {@link DataokeService} 承担（大淘客客户端走 hutool 静态方法，无法用
 * MockRestServiceServer 拦截），因此这里 mock 掉平台层，校验业务编排：
 * 参数兜底、错误码中文化、状态落库、状态变更留痕。</p>
 */
public class MemberFilingServiceTest {

    private static final Long UID = 42L;
    private static final String PID = "mm_1_2_3";

    private DataokeService dataoke;
    private MwTbFilingMapper filingMapper;
    private MwTbFilingLogMapper filingLogMapper;
    private TbMemberFilingService service;

    @Before
    public void setUp() {
        dataoke = mock(DataokeService.class);
        filingMapper = mock(MwTbFilingMapper.class);
        filingLogMapper = mock(MwTbFilingLogMapper.class);

        TbConfig tbConfig = new TbConfig();
        tbConfig.setChannelPid(PID);

        service = new TbMemberFilingService();
        ReflectionTestUtils.setField(service, "dataokeService", dataoke);
        ReflectionTestUtils.setField(service, "tbConfig", tbConfig);
        ReflectionTestUtils.setField(service, "filingMapper", filingMapper);
        ReflectionTestUtils.setField(service, "filingLogMapper", filingLogMapper);
    }

    private static TbPublisherInfoVo publisher(Integer status, String specialPid) {
        String body = "{\"code\":0,\"msg\":\"成功\",\"data\":" + (status == null
                ? "{}"
                : "{\"relation_status\":" + status + ",\"special_pid\":\"" + specialPid + "\"}")
                + "}";
        return com.alibaba.fastjson.JSON.parseObject(body, TbPublisherInfoVo.class);
    }

    private static TbPublisherInfoVo publisherError(String msg) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":-1,\"msg\":\"" + msg + "\",\"data\":null}", TbPublisherInfoVo.class);
    }

    private static TbRelationRecordVo record(String url) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":{\"tb_relation_record\":\"" + url + "\"}}",
                TbRelationRecordVo.class);
    }

    private static TbRelationRecordVo recordError(String msg) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":-1,\"msg\":\"" + msg + "\",\"data\":null}", TbRelationRecordVo.class);
    }

    // ---------------------------------------------------------------
    // 获取备案授权链接
    // ---------------------------------------------------------------

    @Test
    public void relationUrlFallsBackToConfiguredChannelPidAndPersistsLink() {
        when(dataoke.getRelationRecord(eq(3), eq(PID))).thenReturn(record("https://auth.example/abc"));
        when(filingMapper.selectOne(any())).thenReturn(null);

        TbFilingResultVo result = service.getRelationUrl(UID, null);

        assertEquals("https://auth.example/abc", result.getRelationUrl());
        assertFalse(result.isFiled());
        ArgumentCaptor<MwTbFiling> saved = ArgumentCaptor.forClass(MwTbFiling.class);
        verify(filingMapper).insert(saved.capture());
        assertEquals(UID, saved.getValue().getUid());
        assertEquals(PID, saved.getValue().getTbPid());
        assertEquals("https://auth.example/abc", saved.getValue().getRelationUrl());
        assertNotNull(saved.getValue().getUpdateTime());
    }

    @Test
    public void relationUrlIgnoresCallerSuppliedPidAndUsesServerConfiguredOne() {
        // 推客自报 pid 不可信：必须用服务端配置的渠道推广位，否则会把备案关系绑到别人身上
        when(dataoke.getRelationRecord(eq(3), eq(PID))).thenReturn(record("https://auth.example/abc"));
        when(filingMapper.selectOne(any())).thenReturn(null);

        service.getRelationUrl(UID, "mm_hacker_1_2");

        verify(dataoke, times(1)).getRelationRecord(3, PID);
        verify(dataoke, never()).getRelationRecord(3, "mm_hacker_1_2");
    }

    @Test
    public void relationUrlUpstreamFailureIsTranslatedToChineseWithoutRawCode() {
        when(dataoke.getRelationRecord(eq(3), eq(PID))).thenReturn(recordError("sign check fail"));

        try {
            service.getRelationUrl(UID, null);
            fail("上游失败必须抛出业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("备案"));
            assertFalse("不能把上游原始错误码/原文透给前端", expected.getMessage().contains("-1"));
            assertFalse(expected.getMessage(), expected.getMessage().contains("sign check fail"));
        }
        verify(filingMapper, never()).insert(any(MwTbFiling.class));
    }

    @Test
    public void relationUrlRejectsUnknownUserAndMissingChannelPid() {
        try {
            service.getRelationUrl(null, null);
            fail("缺少推客标识必须报错");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("登录"));
        }

        TbConfig blank = new TbConfig();
        blank.setChannelPid("  ");
        ReflectionTestUtils.setField(service, "tbConfig", blank);
        try {
            service.getRelationUrl(UID, null);
            fail("缺少渠道推广位必须报错");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("推广位"));
        }
        verifyNoInteractions(dataoke);
    }

    // ---------------------------------------------------------------
    // 查询备案状态
    // ---------------------------------------------------------------

    @Test
    public void filedStatusIsPersistedWithTimeAndReportedAsFiled() {
        when(dataoke.getPublisherInfo(PID)).thenReturn(publisher(1, "mm_special_1"));
        when(filingMapper.selectOne(any())).thenReturn(null);

        TbFilingResultVo result = service.refreshStatus(UID);

        assertTrue(result.isFiled());
        assertEquals("已备案", result.getStatusText());
        assertEquals(Integer.valueOf(1), result.getRelationStatus());
        assertEquals("mm_special_1", result.getSpecialPid());
        assertNotNull(result.getFiledTime());
        assertNull(result.getHint());

        ArgumentCaptor<MwTbFiling> saved = ArgumentCaptor.forClass(MwTbFiling.class);
        verify(filingMapper).insert(saved.capture());
        assertEquals(Integer.valueOf(1), saved.getValue().getRelationStatus());
        assertNotNull(saved.getValue().getFiledTime());
    }

    @Test
    public void unfiledStatusKeepsRowAndReturnsActionableHint() {
        when(dataoke.getPublisherInfo(PID)).thenReturn(publisher(0, null));
        when(filingMapper.selectOne(any())).thenReturn(null);

        TbFilingResultVo result = service.refreshStatus(UID);

        assertFalse(result.isFiled());
        assertEquals("未备案", result.getStatusText());
        assertNotNull(result.getHint());
        assertTrue(result.getHint(), result.getHint().contains("备案"));
        assertNull(result.getFiledTime());
    }

    @Test
    public void statusChangeWritesAuditLogWithBeforeAndAfter() {
        MwTbFiling existed = MwTbFiling.builder()
                .id(9L).uid(UID).tbPid(PID).relationStatus(0).build();
        when(filingMapper.selectOne(any())).thenReturn(existed);
        when(dataoke.getPublisherInfo(PID)).thenReturn(publisher(1, "mm_special_1"));

        service.refreshStatus(UID);

        ArgumentCaptor<MwTbFilingLog> log = ArgumentCaptor.forClass(MwTbFilingLog.class);
        verify(filingLogMapper).insert(log.capture());
        assertEquals(UID, log.getValue().getUid());
        assertEquals(Integer.valueOf(0), log.getValue().getFromStatus());
        assertEquals(Integer.valueOf(1), log.getValue().getRelationStatus());
        verify(filingMapper).updateById(any(MwTbFiling.class));
        verify(filingMapper, never()).insert(any(MwTbFiling.class));
    }

    @Test
    public void unchangedStatusDoesNotSpamAuditLog() {
        MwTbFiling existed = MwTbFiling.builder()
                .id(9L).uid(UID).tbPid(PID).relationStatus(1).filedTime(new Date()).build();
        when(filingMapper.selectOne(any())).thenReturn(existed);
        when(dataoke.getPublisherInfo(PID)).thenReturn(publisher(1, "mm_special_1"));

        service.refreshStatus(UID);

        verifyNoInteractions(filingLogMapper);
    }

    @Test
    public void statusQueryUpstreamFailureIsTranslatedToChinese() {
        when(dataoke.getPublisherInfo(PID)).thenReturn(publisherError("system error"));

        try {
            service.refreshStatus(UID);
            fail("上游失败必须抛出业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("备案"));
            assertFalse(expected.getMessage().contains("-1"));
            assertFalse(expected.getMessage().contains("system error"));
        }
    }

    // ---------------------------------------------------------------
    // 转链/佣金前置校验
    // ---------------------------------------------------------------

    @Test
    public void assertFiledPassesForFiledUserWithoutHittingUpstream() {
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(UID).relationStatus(1).build());

        service.assertFiled(UID);

        verifyNoInteractions(dataoke);
    }

    @Test
    public void assertFiledRejectsUnfiledUserWithActionableChineseHint() {
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(UID).relationStatus(0).build());

        try {
            service.assertFiled(UID);
            fail("未备案推客必须被拦下");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("备案"));
        }
    }

    @Test
    public void assertFiledTreatsMissingLocalRecordAsUnfiled() {
        when(filingMapper.selectOne(any())).thenReturn(null);

        try {
            service.assertFiled(UID);
            fail("无备案记录必须按未备案处理");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("备案"));
        }
    }

    @Test
    public void assertFiledToleratesAnonymousShopper() {
        // 匿名用户（uid=0/null）不是推客，不应因备案缺失阻断 C 端下单/领奖链路
        service.assertFiled(null);
        service.assertFiled(0L);
        verifyNoInteractions(dataoke);
        verifyNoInteractions(filingMapper);
    }
}
