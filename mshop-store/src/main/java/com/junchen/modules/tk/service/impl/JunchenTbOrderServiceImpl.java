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
import com.junchen.modules.tk.config.TbConfig;
import com.junchen.modules.tk.domain.JunchenTbOrder;
import com.junchen.modules.tk.service.JunchenTbOrderService;
import com.junchen.modules.tk.service.dto.JunchenTbOrderDto;
import com.junchen.modules.tk.service.dto.JunchenTbOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenTbOrderMapper;
import com.junchen.utils.DateUtils;
import com.junchen.utils.FileUtil;
import com.junchen.utils.OrderUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
//@CacheConfig(cacheNames = "junchenTbOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenTbOrderServiceImpl extends BaseServiceImpl<JunchenTbOrderMapper, JunchenTbOrder> implements JunchenTbOrderService {

    private final IGenerator generator;

    @Autowired
    private JunchenTbOrderMapper orderMapper;

    @Resource
    private TbConfig tbConfig;

    @Override
    public PageResult<JunchenTbOrderDto> queryAll(JunchenTbOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenTbOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenTbOrderDto> results = generator.convertPageInfo(page,JunchenTbOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            //淘宝订单状态失效 维权 订单金额为0
            if(OrderUtil.TB_NOT_VALID_ORDER_STATUS.equals(orderDto.getTkStatus())
                    || orderDto.getRefundTag() == 1
                    || orderDto.getAlipayTotalPrice() <= 0) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
                //设置 订单是否是淘礼金0元购订单，前端显示返1元
                orderDto.setIsTlj(orderDto.getAdzoneId().equals(tbConfig.getAdZoneId()));
            }
        });

        return results;
    }
    @Override
    //@Cacheable
    public List<JunchenTbOrder> queryAll(JunchenTbOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenTbOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenTbOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenTbOrderDto junchenTbOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单创建时间", junchenTbOrder.getTkCreateTime());
            map.put("付款时间", junchenTbOrder.getTbPaidTime());
            map.put("付款时间", junchenTbOrder.getTkPaidTime());
            map.put("payPrice",  junchenTbOrder.getPayPrice());
            map.put("pubShareFee",  junchenTbOrder.getPubShareFee());
            map.put("子订单编号", junchenTbOrder.getTradeId());
            map.put("pubShareRate",  junchenTbOrder.getPubShareRate());
            map.put("维权标签，0 含义为非维权 1 含义为维权订单", junchenTbOrder.getRefundTag());
            map.put("实际获得收益的比率", junchenTbOrder.getTkTotalRate());
            map.put("付款预估收入", junchenTbOrder.getPubSharePreFee());
            map.put("买家拍下付款的金额", junchenTbOrder.getAlipayTotalPrice());
            map.put("佣金比率", junchenTbOrder.getTotalCommissionRate());
            map.put("商品标题", junchenTbOrder.getItemTitle());
            map.put("推广位名称", junchenTbOrder.getAdzoneName());
            map.put("pid=mm_1_2_3中的“2”这段数字", junchenTbOrder.getSiteId());
            map.put("pid=mm_1_2_3中的“3”这段数字", junchenTbOrder.getAdzoneId());
            map.put("商品链接", junchenTbOrder.getItemLink());
            map.put("商品单价", junchenTbOrder.getItemPrice());
            map.put("订单所属平台类型，包括天猫、淘宝、聚划算等", junchenTbOrder.getOrderType());
            map.put("店铺名", junchenTbOrder.getSellerShopTitle());
            map.put("媒体名", junchenTbOrder.getSiteName());
            map.put("1）买家超时未付款； 2）买家付款前，买家/卖家取消了订单；3）订单付款后发起售中退款成功；3：订单结算，12：订单付款， 13：订单失效，14：订单成功", junchenTbOrder.getTkStatus());
            map.put("商品id", junchenTbOrder.getItemId());
            map.put("佣金金额=结算金额＊佣金比率", junchenTbOrder.getTotalCommissionFee());
            map.put("会员运营id", junchenTbOrder.getSpecialId());
            map.put("渠道关系id", junchenTbOrder.getRelationId());
            map.put("订单更新时间", junchenTbOrder.getModifiedTime());
            map.put("是否绑定用户，0 未绑定 1 绑定", junchenTbOrder.getBind());
            map.put("用户ID", junchenTbOrder.getUid());
            map.put("积分", junchenTbOrder.getIntegral());
            map.put("cash",  junchenTbOrder.getCash());
            map.put("itemImg",  junchenTbOrder.getItemImg());
            map.put("创建时间", junchenTbOrder.getCreateTime());
            map.put("修改时间", junchenTbOrder.getUpdateTime());
            map.put("删除标识", junchenTbOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }


    @Override
    public List<JunchenTbOrder> getUnbindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenTbOrder::getIsDel, false);
        wrapper.ne(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenTbOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusMinutes(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenTbOrder::getTkCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenTbOrder::getTkCreateTime);
        return orderMapper.selectList(wrapper);
    }
    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenTbOrder::getUid, uid);
        if(innerType != null) {
            wrapper.eq(JunchenTbOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenTbOrder::getBind, 0);
        wrapper.ne(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS);
        wrapper.eq(JunchenTbOrder::getRefundTag, 0);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenTbOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenTbOrder::getIsDel, false);
        wrapper.and(i->i.eq(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS).or().eq(JunchenTbOrder::getRefundTag, 1));
        wrapper.eq(JunchenTbOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenTbOrder::getTkCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
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
    public Set<JunchenTbOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
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

    private LambdaQueryWrapper<JunchenTbOrder> getWrapper(List<Long> uids, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uids, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenTbOrder> getWrapperPro(List<Long> uids, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenTbOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uids != null) {
            wrapperTwo.in(JunchenTbOrder::getUid, uids);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenTbOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenTbOrder::getTkCreateTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenTbOrder::getTkCreateTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.ne(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS);
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
    public JunchenTbOrder mockOrder(Long orderId, Long uid, Date createTime, String shopName,
                                    Double rate, Double fee, Double payPrice,String img,String title,
                                    String link, String itemId, Integer innerType) {
        int orderTypeInt = DateUtils.randomInt(0, 4);
        //如果orderTypeInt==0 淘宝订单
        JunchenTbOrder order = JunchenTbOrder.builder()
                .tradeId(orderId.toString())
                .tradeParentId(orderId)
                .parentId(orderId)
                .tradeId(orderId.toString())
                .tkCreateTime(createTime)
                .tbPaidTime(createTime)
                .tkPaidTime(createTime)
                .refundTag(0)
                .tkStatus(12)
                .siteName("网站")
                .sellerShopTitle(shopName)
                .orderType(orderTypeInt == 0 ? "淘宝":"天猫")
                .adzoneId(147982044L)
                .siteId(39582271L)
                .adzoneName("宝1")
                .pubShareRate("100")
                .pubShareFee("0")
                .pubSharePreFee(fee)
                .totalCommissionFee(0d)
                .tkTotalRate(rate)
                .totalCommissionRate(rate)
                .alipayTotalPrice(payPrice)
                .itemPrice(payPrice)
                .itemImg(img)
                .itemTitle(title)
                .itemLink(link)
                .itemId(itemId)
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

    private LambdaQueryWrapper<JunchenTbOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenTbOrder::getInnerType, 0);

        if(start != null) {
            wrapper.ge(JunchenTbOrder::getTkCreateTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenTbOrder::getTkCreateTime, end);
        }

        return wrapper;
    }

    @Override
    public List<JunchenTbOrder> getSelfUnspreadHbList(Integer limit) {
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenTbOrder::getIsDel, false);
        wrapper.eq(JunchenTbOrder::getBind, 0);
        wrapper.isNotNull(JunchenTbOrder::getUid);
        wrapper.eq(JunchenTbOrder::getInnerType, 0);
        wrapper.ne(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS);
        wrapper.eq(JunchenTbOrder::getRefundTag, 0);
        wrapper.gt(JunchenTbOrder::getAlipayTotalPrice, 0);

        LocalDateTime now = LocalDateTime.now().minusDays(2);
        //查找2天之前的订单
        wrapper.lt(JunchenTbOrder::getTkCreateTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenTbOrder::getUid, uid);
        wrapper.eq(JunchenTbOrder::getInnerType, 0);
        wrapper.le(JunchenTbOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenTbOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

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
        LambdaQueryWrapper<JunchenTbOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenTbOrder::getUid);
        wrapper.eq(JunchenTbOrder::getInnerType, 0);
        wrapper.ne(JunchenTbOrder::getTkStatus, OrderUtil.TB_NOT_VALID_ORDER_STATUS);
        wrapper.gt(JunchenTbOrder::getTotalCommissionRate, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenTbOrder::getTkCreateTime, yesterdayStart);
        wrapper.groupBy(JunchenTbOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
