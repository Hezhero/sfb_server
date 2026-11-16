package com.mailvor.modules.dataoke.rest;

import com.mailvor.api.ApiResult;
import com.mailvor.api.MshopException;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.common.interceptor.UserCheck;
import com.mailvor.modules.tk.service.TbMemberFilingService;
import com.mailvor.modules.tk.vo.TbFilingResultVo;
import com.mailvor.modules.user.domain.MwUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 推客会员备案端点（工单 03）。
 *
 * <p><b>为什么必须有这个能力</b>：淘宝联盟要求推广者与渠道方建立备案关系，
 * 未备案的推广行为无法归因到本平台，佣金从源头就断——这是 B2B2C 模式的
 * 合法性前提。推客在这里拿到专属备案授权链接，完成备案后即可正常推广。</p>
 *
 * <p>两个端点都需要登录：备案关系是「某个推客 ↔ 本平台渠道」的绑定，
 * 必须绑定到登录态下的 uid，不能让调用方自报身份。</p>
 */
@RestController
@RequestMapping("/tao/filing")
@Slf4j
@Api(tags = "推客会员备案")
public class TbFilingController {

    @Resource
    private TbMemberFilingService filingService;

    /**
     * 获取本人的淘宝渠道备案授权链接。
     *
     * @param pid 兼容旧前端的可选入参，**不会被采信**：
     *            实际使用服务端配置的渠道推广位，避免把备案关系绑到他人名下
     */
    @UserCheck
    @GetMapping(value = "/relation/url")
    @ApiOperation(value = "获取会员备案授权链接",
            notes = "返回推客专属的淘宝渠道备案链接，完成备案后推广订单才能归因到本平台")
    public ApiResult<TbFilingResultVo> relationUrl(@RequestParam(required = false) String pid) {
        return ApiResult.ok(filingService.getRelationUrl(currentUid(), pid), "获取成功");
    }

    /** 查询本人的备案状态；未备案时返回可直接展示给用户的中文提示 */
    @UserCheck
    @GetMapping(value = "/status")
    @ApiOperation(value = "查询会员备案状态", notes = "未备案时返回明确的中文引导提示")
    public ApiResult<TbFilingResultVo> status() {
        return ApiResult.ok(filingService.refreshStatus(currentUid()), "获取成功");
    }

    /**
     * 从登录态取 uid。
     *
     * <p>正常情况下 {@code @UserCheck} 已经拦掉未登录请求，这里再兜一层：
     * 备案接口一旦拿到 null uid 就会把不同推客的数据串到一起，必须在入口挡住。</p>
     */
    private Long currentUid() {
        MwUser user = LocalUser.getUser();
        if (user == null || user.getUid() == null || user.getUid() <= 0) {
            throw new MshopException("请先登录后再操作会员备案");
        }
        return user.getUid();
    }
}
