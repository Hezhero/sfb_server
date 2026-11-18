package com.junchen.dataoke;

import com.alibaba.fastjson.JSONObject;
import com.junchen.common.bean.LocalUser;
import com.junchen.modules.dataoke.rest.DataokeController;
import com.junchen.modules.tk.config.TbConfig;
import com.junchen.modules.tk.domain.MwTbFiling;
import com.junchen.modules.tk.service.DataokeService;
import com.junchen.modules.tk.service.TbMemberFilingService;
import com.junchen.modules.tk.service.mapper.MwTbFilingLogMapper;
import com.junchen.modules.tk.service.mapper.MwTbFilingMapper;
import com.junchen.modules.user.domain.MwUser;
import com.junchen.modules.user.service.MwUserUnionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 「未备案提示」契约测试（工单 03，验收标准第 5 条）。
 *
 * <p>推客尝试转链时必须能感知到自己还没备案，否则推广出去了才发现佣金归不到账。
 * 这里校验的是**非阻断式提示**：转链照常返回，但响应里带上
 * {@code filingFiled} / {@code filingHint} 两个字段供 App 弹提示。</p>
 *
 * <p>之所以不做硬阻断：备案记录是本地的，上游没有主动回调，
 * 一个在淘宝已完成备案、但本地还没查过状态的推客，若被直接拦下就等于白丢单。
 * 硬拦只适合「已确知未备案」（本地记录状态为 0）的情况，这条由
 * {@link TbMemberFilingService#isFiled} + {@code filingHint} 组合表达。</p>
 */
public class FilingHintWiringTest {

    private TbMemberFilingService filingService;
    private MwTbFilingMapper filingMapper;
    private DataokeService dataoke;
    private DataokeController controller;
    private TbMemberFilingService realFilingService;

    @Before
    public void setUp() {
        filingMapper = mock(MwTbFilingMapper.class);
        dataoke = mock(DataokeService.class);

        TbConfig tbConfig = new TbConfig();
        tbConfig.setChannelPid("mm_1_2_3");

        realFilingService = new TbMemberFilingService();
        ReflectionTestUtils.setField(realFilingService, "filingMapper", filingMapper);
        ReflectionTestUtils.setField(realFilingService, "filingLogMapper", mock(MwTbFilingLogMapper.class));
        ReflectionTestUtils.setField(realFilingService, "dataokeService", dataoke);
        ReflectionTestUtils.setField(realFilingService, "tbConfig", tbConfig);
        filingService = realFilingService;

        controller = new DataokeController();
        ReflectionTestUtils.setField(controller, "service", dataoke);
        ReflectionTestUtils.setField(controller, "tbConfig", tbConfig);
        ReflectionTestUtils.setField(controller, "userUnionService", mock(MwUserUnionService.class));
        ReflectionTestUtils.setField(controller, "filingService", filingService);

        JSONObject upstream = new JSONObject();
        upstream.put("code", 0);
        JSONObject data = new JSONObject();
        data.put("clickUrl", "https://s.click.taobao.com/abc");
        upstream.put("data", data);
        when(dataoke.goodsWord(any(), any(), any())).thenReturn(upstream);
    }

    @After
    public void clearUser() {
        LocalUser.clear();
    }

    private static void login(Long uid) {
        MwUser user = new MwUser();
        user.setUid(uid);
        LocalUser.set(user, 0);
    }

    // ---------------------------------------------------------------
    // isFiled / hintFor 语义
    // ---------------------------------------------------------------

    @Test
    public void isFiledOnlyTrueWhenLocalRecordSaysFiled() {
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(1L).relationStatus(1).build());
        assertTrue(filingService.isFiled(1L));
    }

    @Test
    public void isFiledFalseWhenRecordUnfiledOrMissing() {
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(2L).relationStatus(0).build());
        assertFalse(filingService.isFiled(2L));

        when(filingMapper.selectOne(any())).thenReturn(null);
        assertFalse(filingService.isFiled(3L));
    }

    @Test
    public void anonymousCallerIsNeverAskedToFile() {
        assertTrue(filingService.isFiled(null));
        assertTrue(filingService.isFiled(0L));
        assertNull(filingService.hintFor(null));
        assertNull(filingService.hintFor(0L));
        verifyNoInteractions(filingMapper);
    }

    @Test
    public void hintIsChineseActionableAndOnlyForUnfiled() {
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(1L).relationStatus(1).build());
        assertNull("已备案不该有提示", filingService.hintFor(1L));

        when(filingMapper.selectOne(any())).thenReturn(null);
        String hint = filingService.hintFor(2L);
        assertNotNull(hint);
        assertTrue(hint, hint.contains("备案"));
        assertTrue("不能暴露上游字段名", !hint.contains("relation_status"));
    }

    // ---------------------------------------------------------------
    // 转链响应里带上提示
    // ---------------------------------------------------------------

    @Test
    public void transLinkResponseCarriesFiledFlagForFiledPublisher() {
        login(42L);
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(42L).relationStatus(1).build());

        JSONObject response = controller.goodsWord("654321", null, null);

        assertEquals(Boolean.TRUE, response.getBoolean("filingFiled"));
        assertNull(response.get("filingHint"));
        assertEquals("https://s.click.taobao.com/abc",
                response.getJSONObject("data").getString("clickUrl"));
    }

    @Test
    public void transLinkResponseCarriesHintForUnfiledPublisherWithoutBlocking() {
        login(42L);
        when(filingMapper.selectOne(any())).thenReturn(
                MwTbFiling.builder().uid(42L).relationStatus(0).build());

        JSONObject response = controller.goodsWord("654321", null, null);

        // 提示必须给出，但转链结果不能被吞掉
        assertEquals(Boolean.FALSE, response.getBoolean("filingFiled"));
        assertNotNull(response.getString("filingHint"));
        assertTrue(response.getString("filingHint").contains("备案"));
        assertEquals("https://s.click.taobao.com/abc",
                response.getJSONObject("data").getString("clickUrl"));
    }

    @Test
    public void anonymousTransLinkIsUnaffected() {
        LocalUser.clear();
        JSONObject response = controller.goodsWord("654321", null, null);

        assertNotNull(response.getJSONObject("data"));
        assertNull("匿名用户不该被要求备案", response.get("filingFiled"));
        verifyNoInteractions(filingMapper);
    }
}
