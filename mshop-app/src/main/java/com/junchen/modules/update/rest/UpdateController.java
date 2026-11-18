/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.update.rest;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.junchen.annotation.AnonymousAccess;
import com.junchen.api.ApiResult;
import com.junchen.modules.logging.aop.log.AppLog;
import com.junchen.modules.shop.domain.MwAppVersion;
import com.junchen.modules.shop.service.MwAppVersionService;
import com.junchen.modules.utils.TkUtil;
import com.junchen.utils.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.junchen.config.PayConfig.PAY_NAME;
import static com.junchen.constant.SystemConfigConstants.UPDATE_CONFIG;

/**
 * <p>
 * 用户提现 前端控制器
 * </p>
 *
 * @author huangyu
 * @since 2019-11-11
 */
@Slf4j
@RestController
@Api(value = "app更新", tags = "APP:app更新")
public class UpdateController {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private MwAppVersionService versionService;

    @AnonymousAccess
    @AppLog(value = "app更新配置", type = 1)
    @GetMapping("/update/config")
    @ApiOperation(value = "app更新配置",notes = "app更新配置")
    public ApiResult<MwAppVersion> updateConfig(){
        MwAppVersion appVersion;

        String updateKey = TkUtil.getMixedPlatformKey(UPDATE_CONFIG);
        Object objS = redisUtils.get(updateKey);
        if(objS == null) {
            appVersion = versionService.getOne(new LambdaQueryWrapper<MwAppVersion>()
                    .eq(MwAppVersion::getEnable, 1).eq(MwAppVersion::getPlatformName, PAY_NAME));
            if(appVersion != null) {
                redisUtils.set(updateKey, appVersion);
            }
        } else {
            appVersion = (MwAppVersion)objS;
        }
        return ApiResult.ok(appVersion);
    }

}

