package com.junchen.modules.quartz.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.junchen.modules.order.service.SuStoreOrderService;
import com.junchen.modules.order.service.dto.UserRefundDto;
import com.junchen.modules.shop.service.MwSystemConfigService;
import com.junchen.modules.tk.domain.*;
import com.junchen.modules.tk.service.*;
import com.junchen.modules.user.config.HbUnlockConfig;
import com.junchen.modules.user.service.MwUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 自购拆红包任务
 *
 * @author Zheng Jie
 * @date 2023-02-04
 */
@Slf4j
@Component
public class SelfOrderHbTask {
    @Resource
    protected JunchenTbOrderService tbOrderService;
    @Resource
    protected JunchenJdOrderService jdOrderService;
    @Resource
    protected JunchenPddOrderService pddOrderService;
    @Resource
    protected JunchenDyOrderService dyOrderService;
    @Resource
    protected JunchenVipOrderService vipOrderService;

    @Resource
    private SuStoreOrderService suStoreOrderService;

    @Resource
    private MwUserService userService;

    @Resource
    private MwSystemConfigService systemConfigService;

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
    protected void run(String paramStr) throws InterruptedException {

        JSONObject jsonObject = JSON.parseObject(paramStr);
//        Integer day = jsonObject.getInteger("day");
        Integer limit = jsonObject.getInteger("limit");
        //找到尚未拆红包，并且已经超过解锁时间的订单
        //todo 每次找到10条 已经绑定用户 未拆红包的 bind=0  并且已经超过默认解锁天数3天的订单

        HbUnlockConfig unlockConfig = systemConfigService.getHbUnlockConfig();
        List<Long> uidList = new ArrayList<>();

        //获取所有订单
        List<JunchenTbOrder> tbOrders = tbOrderService.getSelfUnspreadHbList(limit);
        if(!tbOrders.isEmpty()) {
            uidList.addAll(tbOrders.stream().map(order -> order.getUid()).collect(toList()));
        }

        List<JunchenJdOrder> jdOrders = jdOrderService.getSelfUnspreadHbList(limit);
        if(!jdOrders.isEmpty()) {
            uidList.addAll(jdOrders.stream().map(order -> order.getUid()).collect(toList()));
        }

        List<JunchenPddOrder> pddOrders = pddOrderService.getSelfUnspreadHbList(limit);
        if(!pddOrders.isEmpty()) {
            uidList.addAll(pddOrders.stream().map(order -> order.getUid()).collect(toList()));
        }

        List<JunchenDyOrder> dyOrders = dyOrderService.getSelfUnspreadHbList(limit);
        if(!dyOrders.isEmpty()) {
            uidList.addAll(dyOrders.stream().map(order -> order.getUid()).collect(toList()));
        }

        List<JunchenVipOrder> vipOrders = vipOrderService.getSelfUnspreadHbList(limit);
        if(!vipOrders.isEmpty()) {
            uidList.addAll(vipOrders.stream().map(order -> order.getUid()).collect(toList()));
        }

        if(uidList.isEmpty()) {
            return;
        }
        uidList = uidList.stream().distinct().filter(Objects::nonNull).collect(toList());

        List<UserRefundDto> userDTOS = userService.getUserRefunds(uidList);
        Map<Long, UserRefundDto> userMap = userDTOS.stream().collect(Collectors.toMap(UserRefundDto::getUid, Function.identity()));
        if(!tbOrders.isEmpty()) {
            for(JunchenTbOrder tbOrder : tbOrders) {
                UserRefundDto userRefundDto = userMap.get(tbOrder.getUid());
                Date unlockTime = suStoreOrderService.checkSelfOrder(tbOrder, userRefundDto, unlockConfig);
                if(unlockTime != null) {
                    suStoreOrderService.incMoneyAndBindOrder(tbOrder.getUid(), tbOrder, unlockTime);
                }

            }
        }
        if(!jdOrders.isEmpty()) {
            for(JunchenJdOrder jdOrder : jdOrders) {
                UserRefundDto userRefundDto = userMap.get(jdOrder.getUid());
                Date unlockTime = suStoreOrderService.checkSelfOrder(jdOrder, userRefundDto, unlockConfig);
                if(unlockTime != null) {
                    suStoreOrderService.incMoneyAndBindOrder(jdOrder.getUid(), jdOrder, unlockTime);
                }

            }
        }
        if(!pddOrders.isEmpty()) {
            for(JunchenPddOrder pddOrder : pddOrders) {
                UserRefundDto userRefundDto = userMap.get(pddOrder.getUid());
                Date unlockTime = suStoreOrderService.checkSelfOrder(pddOrder, userRefundDto, unlockConfig);
                if(unlockTime != null) {
                    suStoreOrderService.incMoneyAndBindOrder(pddOrder.getUid(), pddOrder, unlockTime);
                }

            }
        }

        if(!dyOrders.isEmpty()) {
            for(JunchenDyOrder dyOrder : dyOrders) {
                UserRefundDto userRefundDto = userMap.get(dyOrder.getUid());
                Date unlockTime = suStoreOrderService.checkSelfOrder(dyOrder, userRefundDto, unlockConfig);
                if(unlockTime != null) {
                    suStoreOrderService.incMoneyAndBindOrder(dyOrder.getUid(), dyOrder, unlockTime);
                }

            }
        }

        if(!vipOrders.isEmpty()) {
            for(JunchenVipOrder vipOrder : vipOrders) {
                UserRefundDto userRefundDto = userMap.get(vipOrder.getUid());
                Date unlockTime = suStoreOrderService.checkSelfOrder(vipOrder, userRefundDto, unlockConfig);
                if(unlockTime != null) {
                    suStoreOrderService.incMoneyAndBindOrder(vipOrder.getUid(), vipOrder, unlockTime);
                }

            }
        }
    }



}
