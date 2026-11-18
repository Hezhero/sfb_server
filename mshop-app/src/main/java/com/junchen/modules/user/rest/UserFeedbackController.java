/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.rest;

import com.junchen.api.ApiResult;
import com.junchen.common.bean.LocalUser;
import com.junchen.common.interceptor.AuthCheck;
import com.junchen.modules.feedback.domain.MwUserFeedback;
import com.junchen.modules.feedback.service.MwUserFeedbackService;
import com.junchen.modules.user.param.UserFeedbackParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
* @author wangjun
* @date 2024-05-27
*/
@RestController
public class UserFeedbackController {

    @Resource
    private MwUserFeedbackService userFeedbackService;

    @AuthCheck
    @PostMapping(path = "/user/feedback/add")
    @ApiOperation("新增用户反馈")
    public ApiResult<Object> create(@Validated @RequestBody UserFeedbackParam param){
        MwUserFeedback userFeedback = new MwUserFeedback();
        userFeedback.setFeedback(param.getFeedback());
        userFeedback.setUid(LocalUser.getUser().getUid());
        userFeedbackService.save(userFeedback);
        return ApiResult.ok("成功");
    }

}
