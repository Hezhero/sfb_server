package com.junchen.modules.pay.service;

import com.junchen.config.PayConfig;
import com.junchen.modules.pay.dto.PayChannelDto;
import com.junchen.modules.user.domain.MwUserRecharge;
import com.junchen.modules.user.service.MwUserRechargeService;
import com.junchen.modules.user.service.MwUserService;
import com.junchen.utils.OrderUtil;
import com.junchen.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class PayService {

    @Resource
    protected MwUserRechargeService userRechargeService;

    @Resource
    protected MwUserService userService;

    @Resource
    protected MwPayChannelService payChannelService;

    @Resource
    protected RedisUtils redisUtil;

    @Resource
    protected PayConfig payConfig;
    protected void finishRecharge(String orderId, MwUserRecharge recharge, PayChannelDto payChannel) {
        //减少通道剩余额度
        payChannelService.decPrice(recharge.getPrice(), payChannel.getId());
        //完成订单
        userService.setUserLevel(orderId);

        //清空优惠信息
        String couponKey = OrderUtil.getCouponKey(recharge.getPlatform(), recharge.getUid());
        log.info("删除优惠券key：" + couponKey);
        redisUtil.del(couponKey);
    }
}
