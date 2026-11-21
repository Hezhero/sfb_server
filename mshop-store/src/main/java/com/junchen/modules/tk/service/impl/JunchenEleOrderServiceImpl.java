/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.enums.CommonEnum;
import com.junchen.modules.order.service.dto.ChartDataDto;
import com.junchen.modules.tk.domain.JunchenEleOrder;
import com.junchen.modules.tk.service.JunchenEleOrderService;
import com.junchen.modules.tk.service.dto.JunchenEleOrderDto;
import com.junchen.modules.tk.service.dto.JunchenEleOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenEleOrderMapper;
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

import static com.junchen.utils.OrderUtil.ELE_NOT_VALID_ORDER_STATUS;

/**
* @author shenji
* @date 2022-09-07
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "JunchenEleOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenEleOrderServiceImpl extends BaseServiceImpl<JunchenEleOrderMapper, JunchenEleOrder> implements JunchenEleOrderService {

    private final IGenerator generator;
    @Autowired
    private JunchenEleOrderMapper orderMapper;

    @Override
    public PageResult<JunchenEleOrderDto> queryAll(JunchenEleOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenEleOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenEleOrderDto> results = generator.convertPageInfo(page,JunchenEleOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            if(ELE_NOT_VALID_ORDER_STATUS.equals(orderDto.getOrderStatus())
                    || orderDto.getPayPrice() <= 0) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
            }
        });

        return results;
    }



    @Override
    //@Cacheable
    public List<JunchenEleOrder> queryAll(JunchenEleOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenEleOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenEleOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenEleOrderDto JunchenEleOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
//            map.put("订单金额", JunchenEleOrder.getTotalPayAmount());
//            map.put("预估佣金", JunchenEleOrder.getEstimatedTotalCommission());
//            map.put("下单时间", JunchenEleOrder.getPaidTime());
//            map.put("商品标题", JunchenEleOrder.getProductName());
//            map.put("商品id", JunchenEleOrder.getProductId());
//            map.put("佣金比例", JunchenEleOrder.getCommissionRate());
//            map.put("商品主图", JunchenEleOrder.getProductImg());
//            map.put("订单状态，PAY_SUCC", JunchenEleOrder.getFlowPoint());
//            map.put("退款时间", JunchenEleOrder.getRefundTime());
//            map.put("结算时间", JunchenEleOrder.getSettleTime());
//            map.put("商品数量", JunchenEleOrder.getItemNum());
//            map.put("店铺名称", JunchenEleOrder.getShopName());
//            map.put("店铺id", JunchenEleOrder.getShopId());
//            map.put("实际结算金额", JunchenEleOrder.getRealCommission());
//            map.put("媒体名称", JunchenEleOrder.getMediaTypeName());
//            map.put("媒体类型", JunchenEleOrder.getMediaType());
//            map.put(" estimatedCommission",  JunchenEleOrder.getEstimatedCommission());
//            map.put(" estimatedTechServiceFee",  JunchenEleOrder.getEstimatedTechServiceFee());
//            map.put(" estimatedTotalCommission0",  JunchenEleOrder.getEstimatedTotalCommission0());
//            map.put(" app",  JunchenEleOrder.getApp());
            map.put("是否绑定用户，0 未绑定 1 绑定", JunchenEleOrder.getBind());
            map.put("用户ID", JunchenEleOrder.getUid());
            map.put("奖励积分", JunchenEleOrder.getIntegral());
            map.put("是否已提现，0 未提现 1 已提现", JunchenEleOrder.getCash());
            map.put("创建时间", JunchenEleOrder.getCreateTime());
            map.put("修改时间", JunchenEleOrder.getUpdateTime());
            map.put("删除标识", JunchenEleOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<JunchenEleOrder> getUnbindOrderList(Integer prior) {
        //抖音订单状态 PAY_SUCC CONFIRM REFUND
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenEleOrder::getIsDel, false);
        wrapper.ne(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenEleOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusSeconds(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenEleOrder::getPaidTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenEleOrder::getPaidTime);
        return orderMapper.selectList(wrapper);
    }
    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        //抖音订单状态 PAY_SUCC CONFIRM REFUND
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        if(innerType != null) {
            wrapper.eq(JunchenEleOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenEleOrder::getUid, uid);
        wrapper.eq(JunchenEleOrder::getBind, 0);
        wrapper.ne(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenEleOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenEleOrder::getIsDel, false);
        wrapper.eq(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);
        wrapper.eq(JunchenEleOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenEleOrder::getPaidTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        return orderMapper.selectList(wrapper);
    }



    @Override
    public void bindUser(Long uid, String orderId) {
        orderMapper.bindUser(uid, orderId);
    }
    @Override
    public void unbindUser(String id) {
        orderMapper.unbindUser(id);
    }
    @Override
    public Double totalCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        return orderMapper.totalCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()))/100;
    }

    @Override
    public Set<JunchenEleOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
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

    private LambdaQueryWrapper<JunchenEleOrder> getWrapper(List<Long> uid, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uid, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenEleOrder> getWrapperPro(List<Long> uid, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenEleOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uid != null) {
            wrapperTwo.in(JunchenEleOrder::getUid, uid);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenEleOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenEleOrder::getPaidTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenEleOrder::getPaidTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.ne(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);
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
    public JunchenEleOrder mockOrder(String orderId, String productId, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice, String img, String title, Integer innerType) {
        JunchenEleOrder order = JunchenEleOrder.builder()
                .tradeId(orderId)
//                .totalPayAmount(payPrice)
//                .estimatedTotalCommission(fee)
//                .paySuccessTime(createTime)
//                .productName(title)
//                .productId(productId)
//                .commissionRate(rate)
//                .productImg(img)
//                .flowPoint("PAY_SUCC")
//                .itemNum(1)
//                .shopName(shopName)
//                .realCommission(0.0)
//                .mediaTypeName("ProductDetail")
//                .estimatedCommission(fee)
//                .estimatedTechServiceFee(0.0)
//                .estimatedTotalCommission0(fee)
//                .app("抖音")
                .build();
        order.setBind(0);
        order.setUid(uid);
        order.setInnerType(innerType);
        return order;
    }

    @Override
    public void invalidRefundOrders(List<String> ids) {
        for(String id: ids) {
            orderMapper.invalidRefundOrders(id);
        }

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
                return orderMapper.sumFee(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(), skipInvalid));
            case 5:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get30Day(), skipInvalid));
            case 6:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get7Day(), skipInvalid));
        }
        return 0.0;
    }

    @Override
    public Long totalCount(Integer type, List<Long> uid, Integer innerType) {
        return totalCount(type, uid, innerType, false);
    }

    public Long totalCount(Integer type, List<Long> uid, Integer innerType, boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.getToday(), skipInvalid));
            case 2:
                return orderMapper.selectCount(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(), skipInvalid));
            case 3:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.getMonth(), skipInvalid));
            case 4:
                return orderMapper.selectCount(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(), skipInvalid));
            case 5:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.get30Day(), skipInvalid));
            case 6:
                return orderMapper.selectCount(getWrapper(uid, innerType, DateUtils.get7Day(), skipInvalid));
        }
        return 0L;
    }

    @Override
    public Double totalPrice(Integer type, List<Long> uid, Integer innerType) {
        return totalPrice(type, uid, innerType, false);
    }

    public Double totalPrice(Integer type, List<Long> uid, Integer innerType, boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getToday(), skipInvalid));
            case 2:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(), skipInvalid));
            case 3:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getMonth(), skipInvalid));
            case 4:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(), skipInvalid));
            case 5:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get30Day(), skipInvalid));
            case 6:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get7Day(), skipInvalid));
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

    private LambdaQueryWrapper<JunchenEleOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenEleOrder::getInnerType, 0);
        if(start != null) {
            wrapper.ge(JunchenEleOrder::getPaidTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenEleOrder::getPaidTime, end);
        }
        return wrapper;
    }

    @Override
    public List<JunchenEleOrder> getSelfUnspreadHbList(Integer day, Integer limit) {
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenEleOrder::getIsDel, false);
        wrapper.eq(JunchenEleOrder::getBind, 0);
        wrapper.isNotNull(JunchenEleOrder::getUid);
        wrapper.eq(JunchenEleOrder::getInnerType, 0);
        wrapper.ne(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);

        LocalDateTime now = LocalDateTime.now().minusDays(day);
        wrapper.lt(JunchenEleOrder::getPaidTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenEleOrder::getUid, uid);
        wrapper.eq(JunchenEleOrder::getInnerType, 0);
        wrapper.le(JunchenEleOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenEleOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

        return orderMapper.selectCount(wrapper);
    }
    /**
     *
     *
     * @param prior 往前偏移多少天
     * @param scale 佣金比例
     * @return the order check dto
     */
    @Override
    public List<OrderCheckDTO> checkCount(Integer prior, Double scale) {
        LambdaQueryWrapper<JunchenEleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenEleOrder::getUid);
        wrapper.eq(JunchenEleOrder::getInnerType, 0);
        wrapper.ne(JunchenEleOrder::getOrderStatus, ELE_NOT_VALID_ORDER_STATUS);
//        wrapper.gt(JunchenEleOrder::getCommissionRate, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenEleOrder::getPaidTime, yesterdayStart);
        wrapper.groupBy(JunchenEleOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
