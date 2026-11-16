package com.mailvor.dataoke;

import com.mailvor.api.ApiResult;
import com.mailvor.api.MshopException;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.modules.dataoke.rest.TbFilingController;
import com.mailvor.modules.tk.service.TbMemberFilingService;
import com.mailvor.modules.tk.vo.TbFilingResultVo;
import com.mailvor.modules.user.domain.MwUser;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 推客备案 REST 端点契约测试（工单 03）。
 *
 * <p>接缝：Controller 的公开方法。校验的是「对 App 暴露的契约」：
 * 未登录时的表现、uid 如何从登录态推导、{@code redirectUrl} 如何透传给服务层、
 * 返回体结构、异常是否中文化。</p>
 */
public class TbFilingControllerTest {

    private static final Long UID = 42L;
    private static final String REDIRECT_URL = "https://s.click.taobao.com/abc";

    private final TbMemberFilingService filingService = mock(TbMemberFilingService.class);
    private final TbFilingController controller = new TbFilingController();

    public TbFilingControllerTest() {
        ReflectionTestUtils.setField(controller, "filingService", filingService);
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
    // 获取备案链接
    // ---------------------------------------------------------------

    @Test
    public void relationUrlUsesLoggedInUidAndWrapsResultInApiResult() {
        login(UID);
        TbFilingResultVo stub = new TbFilingResultVo();
        stub.setUid(UID);
        stub.setRelationUrl("https://auth.example/abc");
        when(filingService.getRelationUrl(eq(UID), eq(REDIRECT_URL))).thenReturn(stub);

        ApiResult<TbFilingResultVo> result = controller.relationUrl(REDIRECT_URL);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("https://auth.example/abc", result.getData().getRelationUrl());
        verify(filingService).getRelationUrl(eq(UID), eq(REDIRECT_URL));
    }

    @Test
    public void relationUrlPassesRedirectUrlThroughToService() {
        // 官方 get-relation-record 的 redirectUrl 必填：REST 层原样透传给服务层
        login(UID);
        when(filingService.getRelationUrl(any(), any())).thenReturn(new TbFilingResultVo());

        controller.relationUrl("https://s.click.taobao.com/item?id=1");

        verify(filingService).getRelationUrl(UID, "https://s.click.taobao.com/item?id=1");
    }

    @Test
    public void relationUrlWithoutRedirectUrlFailsInChinese() {
        // 不提供默认值：缺参时服务层抛中文化错误，REST 层不吞异常
        login(UID);
        when(filingService.getRelationUrl(eq(UID), eq(null)))
                .thenThrow(new MshopException("缺少备案完成后的跳转商品短链，暂无法生成备案链接"));

        try {
            controller.relationUrl(null);
            fail("缺少 redirectUrl 必须报错");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("跳转"));
        }
    }

    @Test
    public void relationUrlRejectsAnonymousCallerInChinese() {
        LocalUser.clear();
        try {
            controller.relationUrl(REDIRECT_URL);
            fail("未登录必须拒绝");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("登录"));
        }
        verifyNoInteractions(filingService);
    }

    // ---------------------------------------------------------------
    // 查询备案状态
    // ---------------------------------------------------------------

    @Test
    public void statusReturnsFiledStateAndHintForUnfiledUser() {
        login(UID);
        TbFilingResultVo stub = new TbFilingResultVo();
        stub.setUid(UID);
        stub.setFiled(false);
        stub.setStatusText("未备案");
        stub.setHint("您尚未完成淘宝渠道备案");
        when(filingService.refreshStatus(UID)).thenReturn(stub);

        ApiResult<TbFilingResultVo> result = controller.status();

        assertTrue(result.isSuccess());
        assertFalse(result.getData().isFiled());
        assertEquals("未备案", result.getData().getStatusText());
        assertNotNull(result.getData().getHint());
    }

    @Test
    public void statusReturnsOfficialFilingDetailFields() {
        // 官方 get-publisher-info 的 special_id / root_pid / total_count 要能透到前端
        login(UID);
        TbFilingResultVo stub = new TbFilingResultVo();
        stub.setUid(UID);
        stub.setFiled(true);
        stub.setStatusText("已备案");
        stub.setSpecialId(123456789L);
        stub.setRootPid("mm_1_2_3");
        stub.setAccountName("小明");
        stub.setTotalCount(1);
        when(filingService.refreshStatus(UID)).thenReturn(stub);

        TbFilingResultVo data = controller.status().getData();

        assertTrue(data.isFiled());
        assertEquals(Long.valueOf(123456789L), data.getSpecialId());
        assertEquals("mm_1_2_3", data.getRootPid());
        assertEquals("小明", data.getAccountName());
        assertEquals(Integer.valueOf(1), data.getTotalCount());
    }

    @Test
    public void statusRejectsAnonymousCaller() {
        LocalUser.clear();
        try {
            controller.status();
            fail("未登录必须拒绝");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("登录"));
        }
        verifyNoInteractions(filingService);
    }

    @Test
    public void upstreamFailureSurfacesAsChineseBusinessException() {
        login(UID);
        when(filingService.refreshStatus(UID))
                .thenThrow(new MshopException("会员备案信息查询失败，请稍后重试"));
        try {
            controller.status();
            fail("上游失败必须抛出业务异常");
        } catch (MshopException expected) {
            assertTrue(expected.getMessage().contains("备案"));
            assertFalse(expected.getMessage().contains("-1"));
        }
    }
}
