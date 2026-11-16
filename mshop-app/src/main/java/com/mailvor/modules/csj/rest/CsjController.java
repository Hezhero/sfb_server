package com.mailvor.modules.csj.rest;

import com.mailvor.annotation.AnonymousAccess;
import com.mailvor.api.ApiResult;
import com.mailvor.api.MshopException;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.common.interceptor.AuthCheck;
import com.mailvor.common.util.IpUtil;
import com.mailvor.modules.csj.rest.param.CsjRewardParam;
import com.mailvor.modules.csj.service.MwCsjRewardService;
import com.mailvor.modules.user.domain.MwUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 穿山甲激励视频广告
 *
 * <p>APP 端流程：</p>
 * <ol>
 *   <li>调 /csj/config 拿 appId 与广告位 ID，初始化 SDK</li>
 *   <li>展示激励视频，SDK 回调 onRewardVerify 时拿到 transactionId</li>
 *   <li>调 /csj/reward 上报，服务端校验后发积分</li>
 * </ol>
 *
 * @author mailvor
 */
@RestController
@Api(value = "穿山甲广告", tags = "穿山甲：激励视频广告")
@RequestMapping("/csj")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CsjController {

    private final MwCsjRewardService csjRewardService;

    /**
     * 广告配置（匿名可访问，不含任何密钥）
     */
    @AnonymousAccess
    @GetMapping("/config")
    @ApiOperation(value = "获取激励视频配置", notes = "返回 appId、广告位 ID、单次奖励与每日上限")
    public ApiResult<Object> config() {
        return ApiResult.ok(csjRewardService.getClientConfig());
    }

    /**
     * 上报激励视频完成并领取奖励
     */
    @AuthCheck
    @PostMapping("/reward")
    @ApiOperation(value = "领取激励视频奖励", notes = "需登录，transactionId 全局唯一，重复提交不会重复发放")
    public ApiResult<Object> reward(@RequestBody CsjRewardParam param) {
        MwUser user = LocalUser.getUser();
        if (user == null) {
            throw new MshopException("请先登录");
        }
        Map<String, Object> result = csjRewardService.reward(
                user.getUid(), param.getTransactionId(), param.getPosId(), IpUtil.getRequestIp());
        return ApiResult.ok(result);
    }
}
