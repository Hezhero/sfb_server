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
import com.junchen.modules.tk.domain.JunchenDyOrder;
import com.junchen.modules.tk.service.JunchenDyOrderService;
import com.junchen.modules.tk.service.dto.JunchenDyOrderDto;
import com.junchen.modules.tk.service.dto.JunchenDyOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenDyOrderMapper;
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

import static com.junchen.utils.OrderUtil.DY_NOT_VALID_ORDER_STATUS;

/**
* @author shenji
* @date 2022-09-07
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "junchenDyOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenDyOrderServiceImpl extends BaseServiceImpl<JunchenDyOrderMapper, JunchenDyOrder> implements JunchenDyOrderService {

    private final IGenerator generator;
    @Autowired
    private JunchenDyOrderMapper orderMapper;

    @Override
    public PageResult<JunchenDyOrderDto> queryAll(JunchenDyOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenDyOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenDyOrderDto> results = generator.convertPageInfo(page,JunchenDyOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            if(DY_NOT_VALID_ORDER_STATUS.equals(orderDto.getFlowPoint())
                    || orderDto.getTotalPayAmount() <= 0) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
            }
        });

        return results;
    }



    @Override
    //@Cacheable
    public List<JunchenDyOrder> queryAll(JunchenDyOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenDyOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenDyOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenDyOrderDto junchenDyOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单金额", junchenDyOrder.getTotalPayAmount());
            map.put("预估佣金", junchenDyOrder.getEstimatedTotalCommission());
            map.put("下单时间", junchenDyOrder.getPaySuccessTime());
            map.put("商品标题", junchenDyOrder.getProductName());
            map.put("商品id", junchenDyOrder.getProductId());
            map.put("佣金比例", junchenDyOrder.getCommissionRate());
            map.put("商品主图", junchenDyOrder.getProductImg());
            map.put("订单状态，PAY_SUCC", junchenDyOrder.getFlowPoint());
            map.put("退款时间", junchenDyOrder.getRefundTime());
            map.put("结算时间", junchenDyOrder.getSettleTime());
            map.put("商品数量", junchenDyOrder.getItemNum());
            map.put("店铺名称", junchenDyOrder.getShopName());
            map.put("店铺id", junchenDyOrder.getShopId());
            map.put("实际结算金额", junchenDyOrder.getRealCommission());
            map.put("媒体名称", junchenDyOrder.getMediaTypeName());
            map.put("媒体类型", junchenDyOrder.getMediaType());
            map.put(" estimatedCommission",  junchenDyOrder.getEstimatedCommission());
            map.put(" estimatedTechServiceFee",  junchenDyOrder.getEstimatedTechServiceFee());
            map.put(" estimatedTotalCommission0",  junchenDyOrder.getEstimatedTotalCommission0());
            map.put(" app",  junchenDyOrder.getApp());
            map.put("是否绑定用户，0 未绑定 1 绑定", junchenDyOrder.getBind());
            map.put("用户ID", junchenDyOrder.getUid());
            map.put("奖励积分", junchenDyOrder.getIntegral());
            map.put("是否已提现，0 未提现 1 已提现", junchenDyOrder.getCash());
            map.put("创建时间", junchenDyOrder.getCreateTime());
            map.put("修改时间", junchenDyOrder.getUpdateTime());
            map.put("删除标识", junchenDyOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<JunchenDyOrder> getUnbindOrderList(Integer prior) {
        //抖音订单状态 PAY_SUCC CONFIRM REFUND
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenDyOrder::getIsDel, false);
        wrapper.ne(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenDyOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusSeconds(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenDyOrder::getPaySuccessTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenDyOrder::getPaySuccessTime);
        return orderMapper.selectList(wrapper);
    }
    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        //抖音订单状态 PAY_SUCC CONFIRM REFUND
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        if(innerType != null) {
            wrapper.eq(JunchenDyOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenDyOrder::getUid, uid);
        wrapper.eq(JunchenDyOrder::getBind, 0);
        wrapper.ne(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenDyOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenDyOrder::getIsDel, false);
        wrapper.eq(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);
        wrapper.eq(JunchenDyOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenDyOrder::getPaySuccessTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        return orderMapper.selectList(wrapper);
    }



    @Override
    public void bindUser(Long uid, String orderId) {
        orderMapper.bindUser(uid, orderId);
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
    public Set<JunchenDyOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
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

    private LambdaQueryWrapper<JunchenDyOrder> getWrapper(List<Long> uid, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uid, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenDyOrder> getWrapperPro(List<Long> uid, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenDyOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uid != null) {
            wrapperTwo.in(JunchenDyOrder::getUid, uid);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenDyOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenDyOrder::getPaySuccessTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenDyOrder::getPaySuccessTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.ne(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);
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
    public JunchenDyOrder mockOrder(String orderId, String productId, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice, String img, String title, Integer innerType) {
        JunchenDyOrder order = JunchenDyOrder.builder()
                .orderId(orderId)
                .totalPayAmount(payPrice)
                .estimatedTotalCommission(fee)
                .paySuccessTime(createTime)
                .productName(title)
                .productId(productId)
                .commissionRate(rate)
                .productImg(img)
                .flowPoint("PAY_SUCC")
                .itemNum(1)
                .shopName(shopName)
                .realCommission(0.0)
                .mediaTypeName("ProductDetail")
                .estimatedCommission(fee)
                .estimatedTechServiceFee(0.0)
                .estimatedTotalCommission0(fee)
                .app("抖音")
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

    private LambdaQueryWrapper<JunchenDyOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenDyOrder::getInnerType, 0);
        if(start != null) {
            wrapper.ge(JunchenDyOrder::getPaySuccessTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenDyOrder::getPaySuccessTime, end);
        }
        return wrapper;
    }

    @Override
    public List<JunchenDyOrder> getSelfUnspreadHbList(Integer limit) {
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenDyOrder::getIsDel, false);
        wrapper.eq(JunchenDyOrder::getBind, 0);
        wrapper.isNotNull(JunchenDyOrder::getUid);
        wrapper.eq(JunchenDyOrder::getInnerType, 0);
        wrapper.ne(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);

        LocalDateTime now = LocalDateTime.now().minusDays(2);
        wrapper.lt(JunchenDyOrder::getPaySuccessTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenDyOrder::getUid, uid);
        wrapper.eq(JunchenDyOrder::getInnerType, 0);
        wrapper.le(JunchenDyOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenDyOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

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
        LambdaQueryWrapper<JunchenDyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenDyOrder::getUid);
        wrapper.eq(JunchenDyOrder::getInnerType, 0);
        wrapper.ne(JunchenDyOrder::getFlowPoint, DY_NOT_VALID_ORDER_STATUS);
        wrapper.gt(JunchenDyOrder::getCommissionRate, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenDyOrder::getPaySuccessTime, yesterdayStart);
        wrapper.groupBy(JunchenDyOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
