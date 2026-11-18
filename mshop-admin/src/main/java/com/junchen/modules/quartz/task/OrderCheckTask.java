package com.junchen.modules.quartz.task;

import com.junchen.modules.order.service.dto.OrderCheckConfigDto;
import com.junchen.modules.shop.service.MwSystemConfigService;
import com.junchen.modules.tk.service.*;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.user.service.MwUserPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 *  如果前两天有效订单下单超过5单 并且佣金比例超过>30%，退款次数设置为10
 *
 * 有效订单 付款金额大于0的条件
 * @author Zheng Jie
 * @date 2022-10-04
 */
@Slf4j
@Component
public class OrderCheckTask {
    @Resource
    protected JunchenTbOrderService orderService;
    @Resource
    protected JunchenJdOrderService jdOrderService;
    @Resource
    protected JunchenPddOrderService pddOrderService;
    @Resource
    protected JunchenVipOrderService vipOrderService;
    @Resource
    protected JunchenDyOrderService dyOrderService;

    @Resource
    private MwSystemConfigService systemConfigService;

    @Resource
    private MwUserPoolService userPoolService;

    protected void run(String paramStr) {
        OrderCheckConfigDto config = systemConfigService.getOrderCheckConfig();
        //找到满足条件的订单用户
        List<OrderCheckDTO> checks = orderService.checkCount(config.getPrior(), config.getScale());
        List<OrderCheckDTO> jdChecks = jdOrderService.checkCount(config.getPrior(), config.getScale());
        List<OrderCheckDTO> pddChecks = pddOrderService.checkCount(config.getPrior(), config.getScale());
        List<OrderCheckDTO> dyChecks = dyOrderService.checkCount(config.getPrior(), config.getScale());
        List<OrderCheckDTO> vipChecks = vipOrderService.checkCount(config.getPrior(), config.getScale());

        checks.addAll(jdChecks);
        checks.addAll(pddChecks);
        checks.addAll(dyChecks);
        checks.addAll(vipChecks);

        checks.forEach(checkDTO -> {
            //如果订单数量超过一定量 设置为最大拆红包次数
            if(checkDTO.getOrderCount() > config.getCount()){
                userPoolService.setRefund(checkDTO.getUid(), config.getRefund());
            }
        });

    }

}
