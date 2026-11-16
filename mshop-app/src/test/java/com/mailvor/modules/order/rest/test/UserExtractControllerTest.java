package com.mailvor.modules.order.rest.test;

import com.mailvor.common.bean.LocalUser;
import com.mailvor.common.util.JwtToken;
import com.mailvor.config.PayConfig;
import com.mailvor.modules.activity.param.UserExtParam;
import com.mailvor.modules.user.domain.MwUser;
import com.mailvor.modules.user.domain.MwUserUnion;
import com.mailvor.modules.user.rest.UserExtractController;
import com.mailvor.modules.user.service.MwUserService;
import com.mailvor.modules.user.service.MwUserUnionService;
import com.mailvor.modules.user.service.dto.WechatUserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;

/**
 * 提现接口 {@code POST /extract/cash} 的集成测试。
 *
 * <p>该接口带 {@code @NoRepeatSubmit} 与 {@code @AuthCheck}，切面会直接读取
 * {@code Authorization} 头，因此必须先构造合法登录上下文，否则失败原因会变成
 * 「凭证缺失」而不是提现本身的业务规则。
 *
 * <p>测试数据隔离：用户主键由数据库自增分配，不写死固定 uid；整个用例在事务中执行，
 * 结束后回滚，既不依赖库里已有用户，也不残留提现记录。
 */
@SpringBootTest
@Transactional
class UserExtractControllerTest {
    @Autowired
    private UserExtractController orderController;

    @Autowired
    private MwUserService userService;

    @Autowired
    private MwUserUnionService userUnionService;

    private MockHttpServletRequest request;
    private MwUser user;

    @AfterEach
    void tearDown() {
        LocalUser.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @BeforeEach
    void setUp() {
        // 用户主键由数据库自增分配，每个用例都是独立数据，避免与既有数据碰撞
        user = new MwUser();
        user.setUsername("extract-test-" + System.nanoTime());
        user.setRealName("杨玲");
        user.setSpreadUid(1L);
        user.setNowMoney(new BigDecimal("100.00"));
        // 以下为 mw_user 表中无默认值的非空列
        user.setUserType("wechat");
        user.setCode("");
        userService.save(user);

        // 微信提现要求用户已授权微信；getOne(uid) 按 (uid, name=pay.name) 定位记录
        MwUserUnion union = new MwUserUnion();
        union.setUid(user.getUid());
        union.setName(PayConfig.PAY_NAME);
        union.setOpenId("openid-" + user.getUid());
        WechatUserDto wxProfile = new WechatUserDto();
        wxProfile.setOpenid("openid-" + user.getUid());
        union.setWxProfile(wxProfile);
        userUnionService.save(union);

        // 构造真实登录凭证：既要有 LocalUser，也要有带 Authorization 头的请求
        String token = JwtToken.makeToken(user.getUid(), user.getUsername());
        request = new MockHttpServletRequest("POST", "/extract/cash");
        request.addHeader("Authorization", "Bearer " + token);
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LocalUser.set(user, 0);
    }

    @Test
    void addMwUserExtract() {
        UserExtParam param = new UserExtParam();
        param.setExtractType("weixin");
        param.setMoney("24.76");
        param.setName("杨玲");

        // 携带合法登录凭证且具备提现条件时，接口应正常受理而不是抛异常
        orderController.addMwUserExtract(request, param);
    }
}
