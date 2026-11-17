package com.mailvor.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.mailvor.modules.order.service.PlatformOrderRegistry;
import com.mailvor.modules.order.service.SuStoreOrderService;
import com.mailvor.modules.tk.domain.TkOrder;
import com.mailvor.modules.tk.service.*;
import com.mailvor.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 已绑定订单失效后扣除积分
 * @author Zheng Jie
 * @date 2022-10-04
 */
@Slf4j
@Component
public class OrderRefundTask {
    @Resource
    protected MailvorTbOrderService orderService;
    @Resource
    protected MailvorJdOrderService jdOrderService;
    @Resource
    protected MailvorPddOrderService pddOrderService;
    @Resource
    protected MailvorVipOrderService vipOrderService;
    @Resource
    protected MailvorDyOrderService dyOrderService;

    @Resource
    protected MailvorMtOrderService mtOrderService;
    @Resource
    protected MailvorEleOrderService eleOrderService;

    @Resource
    private SuStoreOrderService suStoreOrderService;

    protected void run(String paramStr) {
        //淘宝 京东 拼多多 抖音 唯品会都需执行
        //淘宝 查找30天内 bind=1 并且tkStatus=13的订单，扣除奖励红包， 积分记录, 订单bind改为2
        //todo 其他平台暂未验证
        OrderRefundParam param;
        if(StringUtils.isEmpty(paramStr) || "{}".equals(paramStr)) {
            param = new OrderRefundParam();
        } else {
            param = JSON.parseObject(paramStr, OrderRefundParam.class);
        }
        // 平台清单收在注册表里：新增/漏接一个平台只改注册表一处。
        // 此前这里逐字段枚举 6 个 Service，Ele 全仓库没有任何一处读进退款冲正。
        List<PlatformOrderRegistry.OrderSource> sources = PlatformOrderRegistry.refundableSources(
                orderService, jdOrderService, pddOrderService, vipOrderService,
                dyOrderService, mtOrderService, eleOrderService);
        for (PlatformOrderRegistry.OrderSource source : sources) {
            for (TkOrder order : source.findRefundable(param.getPrior())) {
                if (order.getHb() == null) {
                    continue;
                }
                source.refund(suStoreOrderService, order);
            }
        }
    }

    @Data
    public static class OrderRefundParam {
        /**
         * 多少时间之前的订单 天
         */
        private Integer prior = 30;

    }

}
