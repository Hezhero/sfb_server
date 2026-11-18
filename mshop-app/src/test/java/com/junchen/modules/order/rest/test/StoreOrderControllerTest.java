package com.junchen.modules.order.rest.test;

import com.junchen.api.MshopException;
import com.junchen.common.bean.LocalUser;
import com.junchen.common.util.JwtToken;
import com.junchen.modules.order.param.SubmitOrderParam;
import com.junchen.modules.order.rest.StoreOrderController;
import com.junchen.modules.user.domain.MwUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单找回接口 {@code POST /order/submit} 的集成测试。
 *
 * <p>该接口带 {@code @NoRepeatSubmit} 与 {@code @AuthCheck}，切面会直接读取
 * {@code Authorization} 头，因此测试必须先构造合法的登录上下文
 * （线程绑定的请求 + {@link LocalUser}），否则报错的是凭证缺失而不是业务逻辑。
 *
 * <p>修复后断言接口对外表现出的行为：携合法凭证请求一个不存在的订单号时，
 * 返回业务错误「订单不存在」，而不是鉴权相关的空指针。
 */
@SpringBootTest
class StoreOrderControllerTest {
    private static final Long UID = 8L;

    @Autowired
    private StoreOrderController orderController;

    @AfterEach
    void tearDown() {
        LocalUser.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    /** 构造已登录用户上下文：JWT 请求头绑定到当前线程，并设置 LocalUser。 */
    private void loginAs(Long uid) {
        String token = JwtToken.makeToken(uid, "test-user");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/submit");
        request.addHeader("Authorization", "Bearer " + token);
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MwUser mwUser = new MwUser();
        mwUser.setUid(uid);
        LocalUser.set(mwUser, 0);
    }

    @Test
    void submitOrderWithUnknownOrderIdReportsOrderMissing() {
        loginAs(UID);

        SubmitOrderParam param = new SubmitOrderParam();
        param.setOrderId("252938129656");

        MshopException error = assertThrows(MshopException.class,
                () -> orderController.submitOrder(param));
        assertEquals("订单不存在", error.getMessage());
    }
}
