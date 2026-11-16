package com.mailvor.dataoke;

import com.mailvor.api.ApiResult;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.modules.tk.domain.MwTbFiling;
import com.mailvor.modules.tk.service.TbMemberFilingService;
import com.mailvor.modules.tk.service.mapper.MwTbFilingMapper;
import com.mailvor.modules.user.domain.MwUser;
import com.mailvor.modules.user.rest.UserController;
import com.mailvor.modules.user.service.MwUserService;
import com.mailvor.modules.user.vo.MwCommissionInfoQueryVo;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 「查看佣金时的未备案提示」契约测试（工单 03，验收标准第 5 条）。
 *
 * <p>推客看佣金页面时如果还没备案，必须当场看到提示——否则等到月底
 * 发现没有佣金才追悔。校验的是：提示字段随 commissionInfo 一起返回，
 * 且不改变原有佣金字段。</p>
 *
 * <p>被测对象用 Mockito 的 {@code CALLS_REAL_METHODS} 实例化：
 * {@code UserController} 走 {@code @RequiredArgsConstructor} 构造注入、没有无参构造，
 * 但 {@code commissionInfo} 是真实业务逻辑，必须真跑。</p>
 */
public class CommissionFilingHintTest {

    private static final Long UID = 42L;

    private final MwUserService userService = mock(MwUserService.class);
    private final TbMemberFilingService filingService = mock(TbMemberFilingService.class);
    private final UserController controller = mock(UserController.class, CALLS_REAL_METHODS);

    public CommissionFilingHintTest() {
        ReflectionTestUtils.setField(controller, "mwUserService", userService);
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

    private static MwCommissionInfoQueryVo commission() {
        MwCommissionInfoQueryVo vo = new MwCommissionInfoQueryVo();
        vo.setHbRebateScale(80);
        vo.setTbTimes(1.5);
        return vo;
    }

    @Test
    public void unfiledPublisherSeesHintOnCommissionInfo() {
        login(UID);
        when(userService.getById(UID)).thenReturn(new MwUser().setUid(UID));
        when(userService.getCommissionInfo(any())).thenReturn(commission());
        when(filingService.isFiled(UID)).thenReturn(false);
        when(filingService.hintFor(UID)).thenReturn("您尚未完成淘宝渠道备案");

        MwCommissionInfoQueryVo result = (MwCommissionInfoQueryVo) controller.commissionInfo().getData();

        assertEquals(Boolean.FALSE, result.getTbFilingFiled());
        assertEquals("您尚未完成淘宝渠道备案", result.getTbFilingHint());
        // 原有佣金字段不受影响
        assertEquals(Integer.valueOf(80), result.getHbRebateScale());
        assertEquals(Double.valueOf(1.5), result.getTbTimes());
    }

    @Test
    public void filedPublisherSeesNoHint() {
        login(UID);
        when(userService.getById(UID)).thenReturn(new MwUser().setUid(UID));
        when(userService.getCommissionInfo(any())).thenReturn(commission());
        when(filingService.isFiled(UID)).thenReturn(true);
        when(filingService.hintFor(UID)).thenReturn(null);

        MwCommissionInfoQueryVo result = (MwCommissionInfoQueryVo) controller.commissionInfo().getData();

        assertEquals(Boolean.TRUE, result.getTbFilingFiled());
        assertNull(result.getTbFilingHint());
    }

    @Test
    public void anonymousCallerHasNoFilingFields() {
        LocalUser.clear();
        when(userService.getCommissionInfo(any())).thenReturn(commission());

        MwCommissionInfoQueryVo result = (MwCommissionInfoQueryVo) controller.commissionInfo().getData();

        assertNull(result.getTbFilingFiled());
        assertNull(result.getTbFilingHint());
        verifyNoInteractions(filingService);
    }
}
