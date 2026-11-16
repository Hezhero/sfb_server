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
import static org.mockito.Mockito.*;

/**
 * 推客备案 REST 端点契约测试（工单 03）。
 *
 * <p>接缝：Controller 的公开方法。校验的是「对 App 暴露的契约」：
 * 未登录时的表现、uid 如何从登录态推导、返回体结构、异常是否中文化。</p>
 */
public class TbFilingControllerTest {

    private static final Long UID = 42L;

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
        when(filingService.getRelationUrl(eq(UID), any())).thenReturn(stub);

        ApiResult<TbFilingResultVo> result = controller.relationUrl(null);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("https://auth.example/abc", result.getData().getRelationUrl());
        verify(filingService).getRelationUrl(eq(UID), any());
    }

    @Test
    public void relationUrlRejectsAnonymousCallerInChinese() {
        LocalUser.clear();
        try {
            controller.relationUrl(null);
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
