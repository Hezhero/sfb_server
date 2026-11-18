package com.junchen.dataoke;

import com.junchen.api.MshopException;
import com.junchen.modules.tk.config.TbConfig;
import com.junchen.modules.tk.domain.MwTbFiling;
import com.junchen.modules.tk.domain.MwTbFilingLog;
import com.junchen.modules.tk.service.DataokeService;
import com.junchen.modules.tk.service.TbMemberFilingService;
import com.junchen.modules.tk.service.mapper.MwTbFilingLogMapper;
import com.junchen.modules.tk.service.mapper.MwTbFilingMapper;
import com.junchen.modules.tk.vo.TbFilingResultVo;
import com.junchen.modules.tk.vo.TbPublisherInfoVo;
import com.junchen.modules.tk.vo.TbRelationRecordVo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 会员备案服务契约测试（工单 03）。
 *
 * <p>接缝：{@link TbMemberFilingService} 的公开方法。上游 HTTP 由
 * {@link DataokeService} 承担（大淘客客户端走 hutool 静态方法，无法用
 * MockRestServiceServer 拦截），因此这里 mock 掉平台层，校验业务编排：
 * 官方参数映射（externalId / externalType / opType / redirectUrl）、
 * 错误码中文化、状态落库、状态变更留痕。</p>
 *
 * <p>官方文档：{@code api-d.html?id=135} / {@code id=136}。</p>
 */
public class MemberFilingServiceTest {

    private static final Long UID = 42L;
    /** 官方 externalId：推客 uid 的字符串形式 */
    private static final String EXTERNAL_ID = "42";
    private static final String PID = "mm_1_2_3";
    private static final String REDIRECT_URL = "https://s.click.taobao.com/abc";

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

    /**
     * 按官方 id=136 的报文构造：data.total_count + data.inviter_list[]。
     *
     * @param totalCount 备案记录总数，null 表示 data 为空对象
     * @param inviterJson inviter_list 的 JSON 数组内容（可为空数组）
     */
    private static TbPublisherInfoVo publisher(Integer totalCount, String inviterJson) {
        String data = totalCount == null ? "{}"
                : "{\"total_count\":" + totalCount + ",\"inviter_list\":[" + inviterJson + "]}";
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":" + data + "}", TbPublisherInfoVo.class);
    }

    /** 已备案：total_count=1，inviter_list 一条官方字段记录 */
    private static TbPublisherInfoVo publisherFiled(long specialId, String rootPid) {
        return publisher(1, "{\"account_name\":\"小明\",\"create_date\":\"2026-01-02 03:04:05\","
                + "\"special_id\":" + specialId + ",\"root_pid\":\"" + rootPid + "\"}");
    }

    /** 未备案：total_count=0，inviter_list 空数组 */
    private static TbPublisherInfoVo publisherUnfiled() {
        return publisher(0, "");
    }

    private static TbPublisherInfoVo publisherError(String msg) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":-1,\"msg\":\"" + msg + "\",\"data\":null}", TbPublisherInfoVo.class);
    }

    /** 按官方 id=135 的报文构造：data.url 是唯一字段 */
    private static TbRelationRecordVo record(String url) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":0,\"msg\":\"成功\",\"data\":{\"url\":\"" + url + "\"}}",
                TbRelationRecordVo.class);
    }

    private static TbRelationRecordVo recordError(String msg) {
        return com.alibaba.fastjson.JSON.parseObject(
                "{\"code\":-1,\"msg\":\"" + msg + "\",\"data\":null}", TbRelationRecordVo.class);
    }

    // ---------------------------------------------------------------
    // 获取备案授权链接：官方参数映射
    // ---------------------------------------------------------------

    @Test
    public void relationUrlSendsOfficialParamsAndPersistsLink() {
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getRelationRecord(eq(EXTERNAL_ID), eq(0), eq(1), eq(REDIRECT_URL)))
                .thenReturn(record("https://auth.example/abc"));

        TbFilingResultVo result = service.getRelationUrl(UID, REDIRECT_URL);

        assertEquals("https://auth.example/abc", result.getRelationUrl());
        assertFalse(result.isFiled());

        ArgumentCaptor<MwTbFiling> saved = ArgumentCaptor.forClass(MwTbFiling.class);
        verify(filingMapper).insert(saved.capture());
        assertEquals(UID, saved.getValue().getUid());
        assertEquals("https://auth.example/abc", saved.getValue().getRelationUrl());
        assertNotNull(saved.getValue().getUpdateTime());
    }

    @Test
    public void relationUrlUsesUidAsExternalIdAndFixedExternalTypeZero() {
        // 官方要求 externalId 是私域唯一标识、externalType 是渠道类型（本平台固定 0-其他）
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getRelationRecord(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(record("https://auth.example/abc"));

        service.getRelationUrl(UID, REDIRECT_URL);

        ArgumentCaptor<String> externalId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> externalType = ArgumentCaptor.forClass(Integer.class);
        verify(dataoke).getRelationRecord(externalId.capture(), externalType.capture(),
                anyInt(), eq(REDIRECT_URL));
        assertEquals("42", externalId.getValue());
        assertEquals(Integer.valueOf(0), externalType.getValue());
    }

    @Test
    public void opTypeIsBindForFirstFilingAndUpdateWhenLocalRecordExists() {
        // 首次备案：本地无记录 → opType=1 新绑
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getRelationRecord(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(record("https://auth.example/abc"));
        service.getRelationUrl(UID, REDIRECT_URL);
        verify(dataoke).getRelationRecord(eq(EXTERNAL_ID), eq(0), eq(1), eq(REDIRECT_URL));

        // 已有记录且拿到过授权链接：再次取链接 → opType=2 更新
        reset(dataoke);
        when(dataoke.getRelationRecord(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(record("https://auth.example/def"));
        when(filingMapper.selectOne(any())).thenReturn(MwTbFiling.builder()
                .id(9L).uid(UID).relationStatus(0).relationUrl("https://auth.example/old").build());
        service.getRelationUrl(UID, REDIRECT_URL);
        verify(dataoke, times(1)).getRelationRecord(eq(EXTERNAL_ID), eq(0), eq(2), eq(REDIRECT_URL));
        verify(filingMapper).updateById(any(MwTbFiling.class));
    }

    @Test
    public void opTypeIsUpdateForAlreadyFiledPublisher() {
        // 已备案推客再次取链接 → 也是「更新」
        when(filingMapper.selectOne(any())).thenReturn(MwTbFiling.builder()
                .id(9L).uid(UID).relationStatus(1).build());
        when(dataoke.getRelationRecord(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(record("https://auth.example/abc"));

        service.getRelationUrl(UID, REDIRECT_URL);

        verify(dataoke).getRelationRecord(eq(EXTERNAL_ID), eq(0), eq(2), eq(REDIRECT_URL));
    }

    @Test
    public void relationUrlUpstreamFailureIsTranslatedToChineseWithoutRawCode() {
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getRelationRecord(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(recordError("sign check fail"));

        try {
            service.getRelationUrl(UID, REDIRECT_URL);
            fail("上游失败必须抛出业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("备案"));
            assertFalse("不能把上游原始错误码/原文透给前端", expected.getMessage().contains("-1"));
            assertFalse(expected.getMessage(), expected.getMessage().contains("sign check fail"));
        }
        verify(filingMapper, never()).insert(any(MwTbFiling.class));
    }

    @Test
    public void relationUrlRejectsUnknownUser() {
        try {
            service.getRelationUrl(null, REDIRECT_URL);
            fail("缺少推客标识必须报错");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("登录"));
        }
        verifyNoInteractions(dataoke);
    }

    @Test
    public void relationUrlRejectsMissingOrMalformedRedirectUrl() {
        // 官方 redirectUrl 必填：没有合理默认值（用户完成备案后必须落到具体商品），
        // 缺失或不是 http(s) 时直接给中文化参数错误，不静默兜底。
        String[] bad = {null, "", "   ", "/pages/goods/1", "javascript:alert(1)"};
        for (String redirectUrl : bad) {
            try {
                service.getRelationUrl(UID, redirectUrl);
                fail("redirectUrl 非法必须报错：" + redirectUrl);
            } catch (MshopException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("跳转"));
            }
        }
        verifyNoInteractions(dataoke);
        verifyNoInteractions(filingMapper);
    }

    // ---------------------------------------------------------------
    // 查询备案状态：官方响应字段
    // ---------------------------------------------------------------

    @Test
    public void filedStatusIsPersistedWithOfficialFieldsAndReportedAsFiled() {
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getPublisherInfo(EXTERNAL_ID, 0)).thenReturn(publisherFiled(555L, "mm_root_1_2"));

        TbFilingResultVo result = service.refreshStatus(UID);

        assertTrue(result.isFiled());
        assertEquals("已备案", result.getStatusText());
        assertEquals(Integer.valueOf(1), result.getRelationStatus());
        assertEquals(Long.valueOf(555L), result.getSpecialId());
        assertEquals("mm_root_1_2", result.getRootPid());
        assertEquals("小明", result.getAccountName());
        assertEquals(Integer.valueOf(1), result.getTotalCount());
        assertNotNull(result.getFiledTime());
        assertNull(result.getHint());

        ArgumentCaptor<MwTbFiling> saved = ArgumentCaptor.forClass(MwTbFiling.class);
        verify(filingMapper).insert(saved.capture());
        assertEquals(Integer.valueOf(1), saved.getValue().getRelationStatus());
        assertEquals(Long.valueOf(555L), saved.getValue().getSpecialId());
        assertEquals("mm_root_1_2", saved.getValue().getRootPid());
        assertEquals(Integer.valueOf(1), saved.getValue().getTotalCount());
        assertNotNull(saved.getValue().getFiledTime());
    }

    @Test
    public void publisherInfoQueryUsesOfficialExternalIdAndType() {
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getPublisherInfo(anyString(), anyInt())).thenReturn(publisherUnfiled());

        service.refreshStatus(UID);

        // 官方只接受 externalId + externalType，不再有 pid 参数
        verify(dataoke).getPublisherInfo(eq("42"), eq(0));
    }

    @Test
    public void unfiledStatusKeepsRowAndReturnsActionableHint() {
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getPublisherInfo(EXTERNAL_ID, 0)).thenReturn(publisherUnfiled());

        TbFilingResultVo result = service.refreshStatus(UID);

        assertFalse(result.isFiled());
        assertEquals("未备案", result.getStatusText());
        assertNotNull(result.getHint());
        assertTrue(result.getHint(), result.getHint().contains("备案"));
        assertNull(result.getFiledTime());
        assertEquals(Integer.valueOf(0), result.getTotalCount());
    }

    @Test
    public void multiRecordResponseKeepsLatestInviterAndTotalCount() {
        // inviter_list 多条：只存首条明细 + 总数，避免明细表膨胀
        when(filingMapper.selectOne(any())).thenReturn(null);
        when(dataoke.getPublisherInfo(EXTERNAL_ID, 0)).thenReturn(publisher(2,
                "{\"special_id\":222,\"root_pid\":\"mm_new\"},"
                        + "{\"special_id\":111,\"root_pid\":\"mm_old\"}"));

        TbFilingResultVo result = service.refreshStatus(UID);

        assertEquals(Integer.valueOf(2), result.getTotalCount());
        assertEquals(Long.valueOf(222L), result.getSpecialId());
        assertEquals("mm_new", result.getRootPid());

        ArgumentCaptor<MwTbFiling> saved = ArgumentCaptor.forClass(MwTbFiling.class);
        verify(filingMapper).insert(saved.capture());
        assertEquals(Integer.valueOf(2), saved.getValue().getTotalCount());
        assertEquals(Long.valueOf(222L), saved.getValue().getSpecialId());
        assertEquals("mm_new", saved.getValue().getRootPid());
    }

    @Test
    public void statusChangeWritesAuditLogWithBeforeAndAfter() {
        MwTbFiling existed = MwTbFiling.builder()
                .id(9L).uid(UID).tbPid(PID).relationStatus(0).build();
        when(filingMapper.selectOne(any())).thenReturn(existed);
        when(dataoke.getPublisherInfo(EXTERNAL_ID, 0)).thenReturn(publisherFiled(555L, PID));

        service.refreshStatus(UID);

        ArgumentCaptor<MwTbFilingLog> log = ArgumentCaptor.forClass(MwTbFilingLog.class);
        verify(filingLogMapper).insert(log.capture());
        assertEquals(UID, log.getValue().getUid());
        assertEquals(Integer.valueOf(0), log.getValue().getFromStatus());
        assertEquals(Integer.valueOf(1), log.getValue().getRelationStatus());
        assertEquals(Long.valueOf(555L), log.getValue().getSpecialId());
        verify(filingMapper).updateById(any(MwTbFiling.class));
        verify(filingMapper, never()).insert(any(MwTbFiling.class));
    }

    @Test
    public void unchangedStatusDoesNotSpamAuditLog() {
        MwTbFiling existed = MwTbFiling.builder()
                .id(9L).uid(UID).tbPid(PID).relationStatus(1).filedTime(new Date()).build();
        when(filingMapper.selectOne(any())).thenReturn(existed);
        when(dataoke.getPublisherInfo(EXTERNAL_ID, 0)).thenReturn(publisherFiled(555L, PID));

        service.refreshStatus(UID);

        verifyNoInteractions(filingLogMapper);
    }

    @Test
    public void statusQueryUpstreamFailureIsTranslatedToChinese() {
        when(dataoke.getPublisherInfo(anyString(), anyInt())).thenReturn(publisherError("system error"));

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
