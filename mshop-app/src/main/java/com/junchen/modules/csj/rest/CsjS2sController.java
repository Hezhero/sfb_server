package com.junchen.modules.csj.rest;

import com.junchen.annotation.AnonymousAccess;
import com.junchen.common.util.IpUtil;
import com.junchen.modules.csj.service.CsjS2sService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 穿山甲激励视频「服务端奖励回调（S2S）」端点。
 *
 * <p>由穿山甲服务端以 GET 方式调用，无登录态，因此标注 {@link AnonymousAccess}。
 * 返回体必须是官方约定的裸 JSON {@code {"isValid": true}}，不能用 ApiResult 包装。</p>
 *
 * @see com.junchen.modules.csj.service.CsjS2sService
 */
@RestController
@Api(value = "穿山甲广告S2S回调", tags = "穿山甲：激励视频服务端回调")
@RequestMapping("/csj")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CsjS2sController {

    private final CsjS2sService s2sService;

    @AnonymousAccess
    @GetMapping("/s2s-callback")
    @ApiOperation(value = "穿山甲S2S奖励回调", notes = "官方服务端调用；验签通过后记录可发放白名单")
    public Map<String, Boolean> callback(
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "trans_id", required = false) String transId,
            @RequestParam(value = "reward_name", required = false) String rewardName,
            @RequestParam(value = "reward_amount", required = false) Integer rewardAmount,
            @RequestParam(value = "extra", required = false) String extra,
            @RequestParam(value = "sign", required = false) String sign) {

        boolean ok = s2sService.handleCallback(
                transId, userId, rewardName, rewardAmount, extra, sign, IpUtil.getRequestIp());
        return Collections.singletonMap("isValid", ok);
    }
}
