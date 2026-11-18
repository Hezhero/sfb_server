/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.order.rest;

import com.junchen.api.ApiResult;
import com.junchen.common.bean.LocalUser;
import com.junchen.common.interceptor.AuthCheck;
import com.junchen.modules.logging.aop.log.AppLog;
import com.junchen.modules.order.param.ConfirmIntegralParam;
import com.junchen.modules.order.service.SuStoreOrderService;
import com.junchen.modules.user.domain.MwUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 订单控制器
 * </p>
 *
 * @author huangyu
 * @since 2019-10-27
 */
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(value = "订单模块", tags = "商城：订单模块")
public class PointsOrderController {

    private final SuStoreOrderService suStoreOrderService;

    @Value("${file.path}")
    private String path;


    /**
     * 判断积分是否足够
     */
    @AppLog(value = "积分确认", type = 1)
    @AuthCheck
    @PostMapping("/integral/confirm")
    @ApiOperation(value = "积分确认", notes = "积分确认")
    public ApiResult<Boolean> confirm(@Validated @RequestBody ConfirmIntegralParam param) {
        MwUser mwUser = LocalUser.getUser();
        return ApiResult.ok(suStoreOrderService.confirmIntegral(mwUser, param.getId()));

    }

}

