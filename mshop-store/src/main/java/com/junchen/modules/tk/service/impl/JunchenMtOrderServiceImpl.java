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
import com.junchen.modules.tk.domain.JunchenMtOrder;
import com.junchen.modules.tk.service.JunchenMtOrderService;
import com.junchen.modules.tk.service.dto.JunchenMtOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenMtOrderMapper;
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


/**
* @author shenji
* @date 2022-08-29
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "JunchenMtOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenMtOrderServiceImpl extends BaseServiceImpl<JunchenMtOrderMapper, JunchenMtOrder> implements JunchenMtOrderService {

    private final IGenerator generator;

    @Autowired
    private JunchenMtOrderMapper orderMapper;

    @Override
    public PageResult<JunchenMtOrder> queryAll(JunchenMtOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenMtOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenMtOrder> results = generator.convertPageInfo(page,JunchenMtOrder.class);
        results.getContent().stream().forEach(orderDto -> {
            orderDto.setOrderIdStr(orderDto.getOrderId().toString());
            orderDto.setUniqueItemIdStr(orderDto.getUniqueItemId().toString());
            //淘宝订单状态失效 维权 订单金额为0
            if(OrderUtil.MT_VALID_ORDER_STATUS.contains(orderDto.getItemStatus())
                    && OrderUtil.MT_VALID_ORDER_BIZ_STATUS.contains(orderDto.getItemBizStatus())) {
                orderDto.setRemain("ok");
            } else {
                orderDto.setRemain("err");
            }
        });

        return results;
    }
    @Override
    //@Cacheable
    public List<JunchenMtOrder> queryAll(JunchenMtOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenMtOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenMtOrder> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenMtOrder JunchenMtOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
//            map.put("订单创建时间", JunchenMtOrder.getTkCreateTime());
//            map.put("付款时间", JunchenMtOrder.getTbPaidTime());
//            map.put("付款时间", JunchenMtOrder.getTkPaidTime());
//            map.put("payPrice",  JunchenMtOrder.getPayPrice());
//            map.put("pubShareFee",  JunchenMtOrder.getPubShareFee());
//            map.put("子订单编号", JunchenMtOrder.getTradeId());
//            map.put("pubShareRate",  JunchenMtOrder.getPubShareRate());
//            map.put("维权标签，0 含义为非维权 1 含义为维权订单", JunchenMtOrder.getRefundTag());
//            map.put("实际获得收益的比率", JunchenMtOrder.getTkTotalRate());
//            map.put("付款预估收入", JunchenMtOrder.getPubSharePreFee());
//            map.put("买家拍下付款的金额", JunchenMtOrder.getAlipayTotalPrice());
//            map.put("佣金比率", JunchenMtOrder.getTotalCommissionRate());
//            map.put("商品标题", JunchenMtOrder.getItemTitle());
//            map.put("推广位名称", JunchenMtOrder.getAdzoneName());
//            map.put("pid=mm_1_2_3中的“2”这段数字", JunchenMtOrder.getSiteId());
//            map.put("pid=mm_1_2_3中的“3”这段数字", JunchenMtOrder.getAdzoneId());
//            map.put("商品链接", JunchenMtOrder.getItemLink());
//            map.put("商品单价", JunchenMtOrder.getItemPrice());
//            map.put("订单所属平台类型，包括天猫、淘宝、聚划算等", JunchenMtOrder.getOrderType());
//            map.put("店铺名", JunchenMtOrder.getSellerShopTitle());
//            map.put("媒体名", JunchenMtOrder.getSiteName());
//            map.put("1）买家超时未付款； 2）买家付款前，买家/卖家取消了订单；3）订单付款后发起售中退款成功；3：订单结算，12：订单付款， 13：订单失效，14：订单成功", JunchenMtOrder.getTkStatus());
//            map.put("商品id", JunchenMtOrder.getItemId());
//            map.put("佣金金额=结算金额＊佣金比率", JunchenMtOrder.getTotalCommissionFee());
//            map.put("会员运营id", JunchenMtOrder.getSpecialId());
//            map.put("渠道关系id", JunchenMtOrder.getRelationId());
//            map.put("订单更新时间", JunchenMtOrder.getModifiedTime());
//            map.put("是否绑定用户，0 未绑定 1 绑定", JunchenMtOrder.getBind());
//            map.put("用户ID", JunchenMtOrder.getUid());
//            map.put("积分", JunchenMtOrder.getIntegral());
//            map.put("cash",  JunchenMtOrder.getCash());
//            map.put("itemImg",  JunchenMtOrder.getItemImg());
//            map.put("创建时间", JunchenMtOrder.getCreateTime());
//            map.put("修改时间", JunchenMtOrder.getUpdateTime());
//            map.put("删除标识", JunchenMtOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }


    @Override
    public List<JunchenMtOrder> getUnbindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenMtOrder::getIsDel, false);
        wrapper.in(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS);
        wrapper.in(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS);
        wrapper.isNull(JunchenMtOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusMinutes(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenMtOrder::getOrderPayTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenMtOrder::getOrderPayTime);
        return orderMapper.selectList(wrapper);
    }
    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenMtOrder::getUid, uid);
        if(innerType != null) {
            wrapper.eq(JunchenMtOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenMtOrder::getBind, 0);
        wrapper.in(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS);
        wrapper.in(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenMtOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenMtOrder::getIsDel, false);
        wrapper.and(i->i.notIn(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS)
                .or().notIn(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS));
        wrapper.eq(JunchenMtOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenMtOrder::getOrderPayTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        return orderMapper.selectList(wrapper);
    }

    @Override
    public void bindUser(Long uid, Long id) {
        orderMapper.bindUser(uid, id);
    }
    @Override
    public void refundOrder(Long id) {
        orderMapper.refundOrder(id);
    }
    @Override
    public Double totalCash(Long uid, LocalDateTime time) {
        //付款满七天的订单可以提现
        return orderMapper.totalCash(uid, Date.from(time.atZone( ZoneId.systemDefault()).toInstant()))/100;
    }

    @Override
    public Set<JunchenMtOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
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

    private LambdaQueryWrapper<JunchenMtOrder> getWrapper(List<Long> uids, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uids, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenMtOrder> getWrapperPro(List<Long> uids, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenMtOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uids != null) {
            wrapperTwo.in(JunchenMtOrder::getUid, uids);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenMtOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenMtOrder::getOrderPayTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenMtOrder::getOrderPayTime, end);
        }
        if(skipInvalid) {

            wrapperTwo.and(i->i.notIn(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS)
                    .or().notIn(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS));
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
    public JunchenMtOrder mockOrder(Long orderId, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice,String img,String title,
                                    String link, String itemId, Integer innerType) {
        int orderTypeInt = DateUtils.randomInt(0, 4);
        //如果orderTypeInt==0 淘宝订单
        JunchenMtOrder order = JunchenMtOrder.builder()
//                .tradeId(orderId.toString())
//                .tradeParentId(orderId)
//                .parentId(orderId)
//                .tradeId(orderId.toString())
//                .tkCreateTime(createTime)
//                .tbPaidTime(createTime)
//                .tkPaidTime(createTime)
//                .refundTag(0)
//                .tkStatus(12)
//                .siteName("网站")
//                .sellerShopTitle(shopName)
//                .orderType(orderTypeInt == 0 ? "淘宝":"天猫")
//                .adzoneId(147982044L)
//                .siteId(39582271L)
//                .adzoneName("宝1")
//                .pubShareRate("100")
//                .pubShareFee("0")
//                .pubSharePreFee(fee)
//                .totalCommissionFee(0d)
//                .tkTotalRate(rate)
//                .totalCommissionRate(rate)
//                .alipayTotalPrice(payPrice)
//                .itemPrice(payPrice)
//                .itemImg(img)
//                .itemTitle(title)
//                .itemLink(link)
//                .itemId(itemId)
                .build();
        order.setBind(0);
        order.setUid(uid);
        order.setInnerType(innerType);
        return order;
    }

    @Override
    public void invalidRefundOrders(List<Long> ids) {
        //todo 批量更新sql有问题，暂未解决，暂时单条更新
        for(Long id: ids) {
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

    private LambdaQueryWrapper<JunchenMtOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenMtOrder::getInnerType, 0);

        if(start != null) {
            wrapper.ge(JunchenMtOrder::getOrderPayTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenMtOrder::getOrderPayTime, end);
        }

        return wrapper;
    }

    @Override
    public List<JunchenMtOrder> getSelfUnspreadHbList(Integer day, Integer limit) {
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenMtOrder::getIsDel, false);
        wrapper.eq(JunchenMtOrder::getBind, 0);
        wrapper.isNotNull(JunchenMtOrder::getUid);
        wrapper.eq(JunchenMtOrder::getInnerType, 0);
        wrapper.in(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS);
        wrapper.in(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS);

        LocalDateTime now = LocalDateTime.now().minusDays(day);
        //查找day之前的订单
        wrapper.lt(JunchenMtOrder::getOrderPayTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenMtOrder::getUid, uid);
        wrapper.eq(JunchenMtOrder::getInnerType, 0);
        wrapper.le(JunchenMtOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenMtOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

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
        LambdaQueryWrapper<JunchenMtOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenMtOrder::getUid);
        wrapper.eq(JunchenMtOrder::getInnerType, 0);
        wrapper.in(JunchenMtOrder::getItemStatus, OrderUtil.MT_VALID_ORDER_STATUS);
        wrapper.in(JunchenMtOrder::getItemBizStatus, OrderUtil.MT_VALID_ORDER_BIZ_STATUS);
        wrapper.gt(JunchenMtOrder::getBalanceCommissionRatio, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenMtOrder::getOrderPayTime, yesterdayStart);
        wrapper.groupBy(JunchenMtOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
