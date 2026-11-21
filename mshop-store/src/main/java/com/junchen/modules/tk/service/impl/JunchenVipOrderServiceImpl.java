/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.enums.CommonEnum;
import com.junchen.modules.order.service.dto.ChartDataDto;
import com.junchen.modules.tk.domain.JunchenVipOrder;
import com.junchen.modules.tk.service.JunchenVipOrderService;
import com.junchen.modules.tk.service.dto.JunchenVipOrderDto;
import com.junchen.modules.tk.service.dto.JunchenVipOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenVipOrderMapper;
import com.junchen.modules.user.service.dto.VipOrderDetailDto;
import com.junchen.utils.DateUtils;
import com.junchen.utils.FileUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.junchen.utils.OrderUtil.VIP_NOT_VALID_ORDER_STATUS;

/**
* @author wangjun
* @date 2022-09-07
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "junchenVipOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenVipOrderServiceImpl extends BaseServiceImpl<JunchenVipOrderMapper, JunchenVipOrder> implements JunchenVipOrderService {

    private final IGenerator generator;
    @Autowired
    private JunchenVipOrderMapper orderMapper;

    @Override
    public PageResult<JunchenVipOrderDto> queryAll(JunchenVipOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenVipOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenVipOrderDto> results = generator.convertPageInfo(page,JunchenVipOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            if(VIP_NOT_VALID_ORDER_STATUS.equals(orderDto.getOrderSubStatusName())
                    || Double.parseDouble(orderDto.getTotalCost()) <= 0) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
            }
        });

        return results;
    }


    @Override
    //@Cacheable
    public List<JunchenVipOrder> queryAll(JunchenVipOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenVipOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenVipOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenVipOrderDto junchenVipOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单状态", junchenVipOrder.getOrderSubStatusName());
            map.put("下单时间", junchenVipOrder.getOrderTime());
            map.put("自定义统计参数", junchenVipOrder.getStatParam());
            map.put("订单支付金额:单位元", junchenVipOrder.getTotalCost());
            map.put("appKey", junchenVipOrder.getAppKey());
            map.put("渠道商模式下表示自定义渠道标识；工具商模式下表示pid", junchenVipOrder.getChannelTag());
            map.put("商品佣金金额(元,保留两位小数)", junchenVipOrder.getCommission());
            map.put("入账时间，时间戳，单位毫秒", junchenVipOrder.getCommissionEnterTime());
            map.put("是否预付订单:0-否，1-是", junchenVipOrder.getIsPrepay());
            map.put("订单拆单标识: 0-否，1-是", junchenVipOrder.getIsSplit());
            map.put("订单上次更新时间 时间戳 单位毫秒", junchenVipOrder.getLastUpdateTime());
            map.put("新老客：0-待定，1-新客，2-老客", junchenVipOrder.getNewCustomer());
            map.put("订单来源", junchenVipOrder.getOrderSource());
            map.put("订单归因方式：0-常规推广,1-惊喜红包,2-锁粉,3-超级红包", junchenVipOrder.getOrderTrackReason());
            map.put("推广PID:目前等同于channelTag", junchenVipOrder.getPid());
            map.put("是否自推自买 0-否，1-是", junchenVipOrder.getSelfBuy());
            map.put("订单结算状态 0-未结算,1-已结算", junchenVipOrder.getSettled());
            map.put("订单状态:0-不合格，1-待定，2-已完结", junchenVipOrder.getStatus());
            map.put("商品json array信息", junchenVipOrder.getDetailList());
            map.put("是否绑定用户，0 未绑定 1 绑定", junchenVipOrder.getBind());
            map.put("用户ID", junchenVipOrder.getUid());
            map.put("奖励积分", junchenVipOrder.getIntegral());
            map.put("是否已提现，0 未提现 1 已提现", junchenVipOrder.getCash());
            map.put("创建时间", junchenVipOrder.getCreateTime());
            map.put("修改时间", junchenVipOrder.getUpdateTime());
            map.put("删除标识", junchenVipOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<JunchenVipOrder> getUnbindOrderList(Long prior) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenVipOrder::getIsDel, false);
        wrapper.ne(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenVipOrder::getUid);
        LocalDateTime now = LocalDateTime.now().minusSeconds(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenVipOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenVipOrder::getOrderTime);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        if(innerType != null) {
            wrapper.eq(JunchenVipOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenVipOrder::getUid, uid);
        wrapper.eq(JunchenVipOrder::getBind, 0);
        wrapper.ne(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);

        return orderMapper.selectCount(wrapper) > 0;
    }
    @Override
    public List<JunchenVipOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenVipOrder::getIsDel, false);
        wrapper.eq(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);
        wrapper.eq(JunchenVipOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenVipOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        return orderMapper.selectList(wrapper);
    }

    @Override
    public void bindUser(Long uid, String orderSn) {
        orderMapper.bindUser(uid, orderSn);
    }
    @Override
    public void refundOrder(String id) {
        orderMapper.refundOrder(id);
    }
    @Override
    public Double totalCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        return orderMapper.totalCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()))/100;
    }

    @Override
    public Set<JunchenVipOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
        return orderMapper.selectCashOrderId(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()));
    }


    @Override
    public void updateCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        orderMapper.updateCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()));
    }

    @Override
    public Double sumTotalPrice(){
        return orderMapper.sumTotalPrice();
    }

    @Override
    public Double sumTotalFee(){
        return orderMapper.sumTotalFee();
    }

    private LambdaQueryWrapper<JunchenVipOrder> getWrapper(List<Long> uid, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uid, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenVipOrder> getWrapperPro(List<Long> uid, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenVipOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uid != null) {
            wrapperTwo.in(JunchenVipOrder::getUid, uid);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenVipOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenVipOrder::getOrderTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenVipOrder::getOrderTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.ne(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);
        }
        return wrapperTwo;
    }

    @Override
    public List<ChartDataDto> chartList(Date time){
        return orderMapper.chartList(time);
    }
    @Override
    public List<ChartDataDto> chartListT(Date time){
        return orderMapper.chartListT(time);
    }

    @Override
    public List<ChartDataDto> chartListFee(Date time){
        return orderMapper.chartListFee(time);
    }


    @Override
    public JunchenVipOrder mockOrder(String orderId, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice, String img, String title,
                                    String itemId, Integer innerType) {
        ArrayList<JSONObject> detailDtos = new ArrayList<>(1);
        String feeStr = String.format("%.2f", fee);
        VipOrderDetailDto detailDto = VipOrderDetailDto.builder()
                .goodsName(title)
                .goodsThumb(img)
                .spuId(itemId)
                .sizeId(itemId)
                .status(2)
                .goodsId(itemId)
                .commCode("338")
                .commName("其他")
                .commission(feeStr)
                .goodsCount(1)
                .brandStoreName(shopName)
                .orderSource("app-android")
                .commissionRate(rate+"")
                .goodsFinalPrice(payPrice+"")
                .isSubsidyTaskOrder(false)
                .commissionTotalCost(payPrice + "")
                .build();
        detailDtos.add(JSON.parseObject(JSON.toJSONString(detailDto)));
        JunchenVipOrder order = JunchenVipOrder.builder()
                .orderSn(orderId)
                .orderSubStatusName("已付款")
                .orderTime(createTime)
                .statParam(4+"")
                .totalCost(payPrice + "")
                .appKey("0a0007bb")
                .channelTag("5cb48570ea42f03970c1c40a2b8e2146")
                .commission(feeStr)
                .isPrepay(false)
                .isSplit(false)
                .lastUpdateTime(createTime)
                .newCustomer(2)
                .orderSource("app-android")
                .orderTrackReason(0)
                .pid("5cb48570ea42f03970c1c40a2b8e2146")
                .selfBuy(0)
                .settled(0)
                .status(1)
                .detailList(detailDtos)
                .build();
        order.setBind(0);
        order.setUid(uid);
        order.setInnerType(innerType);
        return order;
    }

    @Override
    public void invalidRefundOrders(List<String> ids) {
        for(String id : ids) {
            orderMapper.invalidRefundOrders(id);
        }
    }
    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType){
        return totalFee(type, uid, innerType, false);
    }
    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType, boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.getToday(), skipInvalid));
            case 2:
                return orderMapper.sumFee(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(), skipInvalid));
            case 3:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.getMonth(), skipInvalid));
            case 4:
                return orderMapper.sumFee(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(),skipInvalid));
            case 5:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get30Day(),skipInvalid));
            case 6:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get7Day(),skipInvalid));
        }
        return 0.0;
    }
    public Long totalCount(Integer type, List<Long> uid, Integer innerType){
        return totalCount(type, uid, innerType, false);
    }
    @Override
    public Long totalCount(Integer type, List<Long> uid, Integer innerType, boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.getToday(),skipInvalid));
            case 2:
                return orderMapper.selectCount(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(),skipInvalid));
            case 3:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.getMonth(),skipInvalid));
            case 4:
                return orderMapper.selectCount(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(),skipInvalid));
            case 5:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.get30Day(),skipInvalid));
            case 6:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.get7Day(),skipInvalid));
        }
        return 0L;
    }
    @Override
    public Double totalPrice(Integer type, List<Long> uid, Integer innerType){
        return totalPrice(type, uid, innerType, false);
    }
    @Override
    public Double totalPrice(Integer type, List<Long> uid, Integer innerType, boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getToday(),skipInvalid));
            case 2:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(),skipInvalid));
            case 3:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getMonth(),skipInvalid));
            case 4:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(),skipInvalid));
            case 5:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get30Day(),skipInvalid));
            case 6:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get7Day(),skipInvalid));
        }
        return 0.0;
    }


    @Override
    public Double totalHb(Integer type){
        switch (type){
            case 1:
                return orderMapper.sumHb(getHbWrapper(DateUtils.getToday(), null));
            case 2:
                return orderMapper.sumHb(getHbWrapper(DateUtils.getProDay(), DateUtils.getToday()));
            case 3:
                return orderMapper.sumHb(getHbWrapper(DateUtils.getMonth(), null));
            case 4:
                return orderMapper.sumHb(getHbWrapper(DateUtils.getProMonth(), DateUtils.getMonth()));
            case 5:
                return orderMapper.sumHb(getHbWrapper(DateUtils.get30Day(), null));
            case 6:
                return orderMapper.sumHb(getHbWrapper(DateUtils.get7Day(), null));
        }
        return 0.0;
    }

    private LambdaQueryWrapper<JunchenVipOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenVipOrder::getInnerType, 0);

        if(start != null) {
            wrapper.ge(JunchenVipOrder::getOrderTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenVipOrder::getOrderTime, end);
        }

        return wrapper;
    }

    @Override
    public List<JunchenVipOrder> getSelfUnspreadHbList(Integer limit) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenVipOrder::getBind, 0);
        wrapper.isNotNull(JunchenVipOrder::getUid);
        wrapper.eq(JunchenVipOrder::getInnerType, 0);

        wrapper.ne(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);

        LocalDateTime now = LocalDateTime.now().minusDays(2);
        wrapper.le(JunchenVipOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenVipOrder::getUid, uid);
        wrapper.eq(JunchenVipOrder::getInnerType, 0);
        wrapper.le(JunchenVipOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenVipOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

        return orderMapper.selectCount(wrapper);
    }    /**
     *
     *
     * @param prior 往前偏移多少天
     * @param scale 佣金比例
     * @return the order check dto
     */
    @Override
    public List<OrderCheckDTO> checkCount(Integer prior, Double scale) {
        LambdaQueryWrapper<JunchenVipOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenVipOrder::getUid);
        wrapper.eq(JunchenVipOrder::getInnerType, 0);
        wrapper.ne(JunchenVipOrder::getOrderSubStatusName, VIP_NOT_VALID_ORDER_STATUS);
        wrapper.gt(JunchenVipOrder::getCommissionRate, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenVipOrder::getOrderTime, yesterdayStart);
        wrapper.groupBy(JunchenVipOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
