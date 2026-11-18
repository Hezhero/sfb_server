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
import com.junchen.modules.tk.domain.JunchenPddOrder;
import com.junchen.modules.tk.service.JunchenPddOrderService;
import com.junchen.modules.tk.service.dto.JunchenPddOrderDto;
import com.junchen.modules.tk.service.dto.JunchenPddOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenPddOrderMapper;
import com.junchen.utils.DateUtils;
import com.junchen.utils.FileUtil;
import com.junchen.utils.OrderUtil;
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

import static com.junchen.utils.OrderUtil.PDD_VALID_ORDER_STATUS;

/**
* @author shenji
* @date 2022-09-06
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "junchenPddOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenPddOrderServiceImpl extends BaseServiceImpl<JunchenPddOrderMapper, JunchenPddOrder> implements JunchenPddOrderService {

    private final IGenerator generator;

    @Autowired
    private JunchenPddOrderMapper orderMapper;

    @Override
    public PageResult<JunchenPddOrderDto> queryAll(JunchenPddOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenPddOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenPddOrderDto> results = generator.convertPageInfo(page,JunchenPddOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            if(!OrderUtil.PDD_VALID_ORDER_STATUS.contains(orderDto.getOrderStatus())
                    || orderDto.getOrderAmount() <= 0
                    || orderDto.getPriceCompareStatus() == 1) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
            }
        });
        return results;
    }
    @Override
    //@Cacheable
    public List<JunchenPddOrder> queryAll(JunchenPddOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenPddOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenPddOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenPddOrderDto junchenPddOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单状态：0-已支付；1-已成团；2-确认收货；3-审核成功；4-审核失败（不可提现）；5-已经结算 ;10-已处罚", junchenPddOrder.getOrderStatus());
            map.put("订单状态描述", junchenPddOrder.getOrderStatusDesc());
            map.put("比价状态：0：正常，1：比价", junchenPddOrder.getPriceCompareStatus());
            map.put("佣金金额，单位为分", junchenPddOrder.getPromotionAmount());
            map.put("自定义参数，转链时传入", junchenPddOrder.getCustomParameters());
            map.put("商品ID", junchenPddOrder.getGoodsId());
            map.put("商品名称", junchenPddOrder.getGoodsName());
            map.put("订单中sku的单件价格，单位为分", junchenPddOrder.getGoodsPrice());
            map.put("商品数量", junchenPddOrder.getGoodsQuantity());
            map.put("goodsSign是加密后的goodsId，goodsId已下线，请使用goodsSign来替代", junchenPddOrder.getGoodsSign());
            map.put("商品缩略图", junchenPddOrder.getGoodsThumbnailUrl());
            map.put("成团编号", junchenPddOrder.getGroupId());
            map.put("店铺id", junchenPddOrder.getMallId());
            map.put("店铺名称", junchenPddOrder.getMallName());
            map.put("是否直推 ，1表示是，0表示否", junchenPddOrder.getIsDirect());
            map.put("实际支付金额，单位为分", junchenPddOrder.getOrderAmount());
            map.put("多多客工具id", junchenPddOrder.getAuthDuoId());
            map.put("预判断是否为代购订单，-1（默认）表示未出结果，0表示预判不是代购订单，1表示代购订单，具体请以最后审核状态为准", junchenPddOrder.getBandanRiskConsult());
            map.put("结算批次号", junchenPddOrder.getBatchNo());
            map.put("订单关联礼金活动Id", junchenPddOrder.getCashGiftId());
            map.put("是否是 cpa 新用户，1表示是，0表示否", junchenPddOrder.getCpaNew());
            map.put("订单审核失败/惩罚原因", junchenPddOrder.getFailReason());
            map.put("商品一级类目名称", junchenPddOrder.getGoodsCategoryName());
            map.put("非补贴订单原因", junchenPddOrder.getNoSubsidyReason());
            map.put("订单生成时间，UNIX时间戳", junchenPddOrder.getOrderCreateTime());
            map.put("成团时间", junchenPddOrder.getOrderGroupSuccessTime());
            map.put("最后更新时间", junchenPddOrder.getOrderModifyAt());
            map.put("支付时间", junchenPddOrder.getOrderPayTime());
            map.put("确认收货时间", junchenPddOrder.getOrderReceiveTime());
            map.put("结算时间", junchenPddOrder.getOrderSettleTime());
            map.put("审核时间", junchenPddOrder.getOrderVerifyTime());
            map.put("推广位ID", junchenPddOrder.getPId());
            map.put("平台券金额，表示该订单使用的平台券金额，单位分", junchenPddOrder.getPlatformDiscount());
            map.put("佣金比例，千分比", junchenPddOrder.getPromotionRate());
            map.put("超级红包补贴类型：0-非红包补贴订单，1-季度新用户补贴", junchenPddOrder.getRedPacketType());
            map.put("直播间订单推广duoId", junchenPddOrder.getSepDuoId());
            map.put("直播间推广佣金", junchenPddOrder.getSepMarketFee());
            map.put("直播间推广自定义参数", junchenPddOrder.getSepParameters());
            map.put("直播间订单推广位", junchenPddOrder.getSepPid());
            map.put("直播间推广佣金比例", junchenPddOrder.getSepRate());
            map.put("招商分成服务费金额，单位为分", junchenPddOrder.getShareAmount());
            map.put("招商分成服务费比例，千分比", junchenPddOrder.getShareRate());
            map.put("优势渠道专属商品补贴金额，单位为分", junchenPddOrder.getSubsidyAmount());
            map.put("等级补贴给渠道的收入补贴，不允许直接给下级代理展示，单位为分", junchenPddOrder.getSubsidyDuoAmountLevel());
            map.put("官方活动给渠道的收入补贴金额，不允许直接给下级代理展示，单位为分", junchenPddOrder.getSubsidyDuoAmountTenMillion());
            map.put("补贴订单备注", junchenPddOrder.getSubsidyOrderRemark());
            map.put("订单补贴类型：0-非补贴订单，1-千万补贴，2-社群补贴，3-多多星选，4-品牌优选，5-千万神券", junchenPddOrder.getSubsidyType());
            map.put("下单场景类型：0-单品推广，1-红包活动推广，4-多多进宝商城推广，7-今日爆款，8-品牌清仓，9-1.9包邮，77-刮刮卡活动推广，94-充值中心，101-品牌黑卡，103-百亿补贴频道，104-内购清单频道，105-超级红包", junchenPddOrder.getType());
            map.put("招商多多客id", junchenPddOrder.getZsDuoId());
            map.put("是否绑定用户，0 未绑定 1 绑定", junchenPddOrder.getBind());
            map.put("用户ID", junchenPddOrder.getUid());
            map.put("奖励积分", junchenPddOrder.getIntegral());
            map.put("是否已提现，0 未提现 1 已提现", junchenPddOrder.getCash());
            map.put("创建时间", junchenPddOrder.getCreateTime());
            map.put("修改时间", junchenPddOrder.getUpdateTime());
            map.put("删除标识", junchenPddOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<JunchenPddOrder> getUnbindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenPddOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue());
        wrapper.in(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenPddOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusMinutes(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenPddOrder::getOrderCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenPddOrder::getOrderCreateTime);
        return orderMapper.selectList(wrapper);
    }
    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        if(innerType != null) {
            wrapper.eq(JunchenPddOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenPddOrder::getUid, uid);
        wrapper.eq(JunchenPddOrder::getBind, 0);

        wrapper.in(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
        wrapper.eq(JunchenPddOrder::getPriceCompareStatus, 0);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenPddOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenPddOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue());
        wrapper.notIn(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
        wrapper.eq(JunchenPddOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenPddOrder::getOrderCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        return orderMapper.selectList(wrapper);
    }


    @Override
    public void bindUser(Long uid, String orderSn) {
        orderMapper.bindUser(uid, orderSn);
    }
    @Override
    public void refundOrder(String orderSn) {
        orderMapper.refundOrder(orderSn);
    }
    @Override
    public Double totalCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        return orderMapper.totalCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()))/100;
    }
    @Override
    public Set<JunchenPddOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
        return orderMapper.selectCashOrderId(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()));
    }

    @Override
    public void updateCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        orderMapper.updateCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()));
    }

    @Override
    public Double sumTotalPrice(){
        return orderMapper.sumTotalPrice()/100;
    }

    @Override
    public Double sumTotalFee(){
        return orderMapper.sumTotalFee()/100;
    }

    private LambdaQueryWrapper<JunchenPddOrder> getWrapper(List<Long> uid, Integer innerType, DateTime start , boolean skipInvalid) {
        return getWrapperPro(uid, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenPddOrder> getWrapperPro(List<Long> uid, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenPddOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uid != null) {
            wrapperTwo.in(JunchenPddOrder::getUid, uid);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenPddOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenPddOrder::getOrderCreateTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenPddOrder::getOrderCreateTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.in(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
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
    public JunchenPddOrder mockOrder(String orderId, String goodsSign, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice, String img, String title, Integer innerType) {
        Long price = (long)(payPrice*100);
        JunchenPddOrder order = JunchenPddOrder.builder()
                .orderSn(orderId)
                .orderStatus(1)
                .orderStatusDesc("已成团")
                .priceCompareStatus(0)
                .promotionAmount((long)(fee*100))
                .goodsId(Long.parseLong(orderId.split("-")[1]))
                .goodsName(title)
                .goodsPrice(price)
                .orderAmount(price)
                .goodsQuantity(1L)
                .goodsSign(goodsSign)
                .goodsThumbnailUrl(img)
                .groupId(System.currentTimeMillis())
                .mallId(000000000L)
                .mallName(shopName)
                .isDirect(0)
                .authDuoId(10916965L)
                .bandanRiskConsult(-1)
                .cpaNew(0)
                .goodsCategoryName("其他")
                .orderCreateTime(createTime)
                .orderGroupSuccessTime(createTime)
                .orderModifyAt(createTime)
                .orderPayTime(createTime)
                .pId("1784892_252288888")
                .promotionRate((long)(rate*10))
                .redPacketType(0)
                .sepDuoId(0L)
                .sepMarketFee(0)
                .sepRate(0)
                .shareAmount(0)
                .shareRate(0)
                .subsidyAmount(0)
                .subsidyDuoAmountLevel(0)
                .subsidyDuoAmountTenMillion(0)
                .subsidyType(0)
                .type(64)
                .zsDuoId(0L)
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
    public Double totalFee(Integer type, List<Long> uid, Integer innerType , boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.getToday(), skipInvalid))/100;
            case 2:
                return orderMapper.sumFee(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(), skipInvalid))/100;
            case 3:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.getMonth(), skipInvalid))/100;
            case 4:
                return orderMapper.sumFee(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(), skipInvalid))/100;
            case 5:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get30Day(), skipInvalid))/100;
            case 6:
                return orderMapper.sumFee(getWrapper(uid, innerType, DateUtils.get7Day(), skipInvalid))/100;
        }
        return 0.0;
    }

    @Override
    public Long totalCount(Integer type, List<Long> uid, Integer innerType) {
        return totalCount(type, uid, innerType, false);
    }

    public Long totalCount(Integer type, List<Long> uid, Integer innerType , boolean skipInvalid){
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

    public Double totalPrice(Integer type, List<Long> uid, Integer innerType , boolean skipInvalid){
        switch (type){
            case 1:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getToday(), skipInvalid))/100;
            case 2:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProDay(), DateUtils.getToday(), skipInvalid))/100;
            case 3:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.getMonth(), skipInvalid))/100;
            case 4:
                return orderMapper.sumPrice(getWrapperPro(uid, innerType, DateUtils.getProMonth(), DateUtils.getMonth(), skipInvalid))/100;
            case 5:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get30Day(), skipInvalid))/100;
            case 6:
                return orderMapper.sumPrice(getWrapper(uid, innerType, DateUtils.get7Day(), skipInvalid))/100;
        }
        return 0.0;
    }
    public static void main(String[] args) {
        System.out.println((long)(10.24*100));
        System.out.println(String.valueOf(10.24*100));
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

    private LambdaQueryWrapper<JunchenPddOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenPddOrder::getInnerType, 0);

        if(start != null) {
            wrapper.ge(JunchenPddOrder::getOrderCreateTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenPddOrder::getOrderCreateTime, end);
        }

        return wrapper;
    }


    @Override
    public List<JunchenPddOrder> getSelfUnspreadHbList(Integer limit) {
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenPddOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue());
        wrapper.eq(JunchenPddOrder::getBind, 0);
        wrapper.isNotNull(JunchenPddOrder::getUid);
        wrapper.eq(JunchenPddOrder::getInnerType, 0);

        wrapper.in(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
        wrapper.eq(JunchenPddOrder::getPriceCompareStatus, 0);

        LocalDateTime now = LocalDateTime.now().minusDays(2);
        wrapper.lt(JunchenPddOrder::getOrderCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenPddOrder::getUid, uid);
        wrapper.eq(JunchenPddOrder::getInnerType, 0);
        wrapper.le(JunchenPddOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenPddOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

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
        LambdaQueryWrapper<JunchenPddOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenPddOrder::getUid);
        wrapper.eq(JunchenPddOrder::getInnerType, 0);
        wrapper.in(JunchenPddOrder::getOrderStatus, PDD_VALID_ORDER_STATUS);
        wrapper.gt(JunchenPddOrder::getPromotionRate, scale * 10);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenPddOrder::getOrderCreateTime, yesterdayStart);
        wrapper.groupBy(JunchenPddOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
