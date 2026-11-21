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
import com.junchen.modules.tk.domain.JunchenJdOrder;
import com.junchen.modules.tk.service.JunchenJdOrderService;
import com.junchen.modules.tk.service.dto.JunchenJdOrderDto;
import com.junchen.modules.tk.service.dto.JunchenJdOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import com.junchen.modules.tk.service.mapper.JunchenJdOrderMapper;
import com.junchen.modules.user.service.dto.GoodsInfoDto;
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

import static com.junchen.utils.OrderUtil.JD_VALID_ORDER_STATUS;

/**
* @author shenji
* @date 2022-09-05
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "junchenJdOrder")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class JunchenJdOrderServiceImpl extends BaseServiceImpl<JunchenJdOrderMapper, JunchenJdOrder> implements JunchenJdOrderService {

    private final IGenerator generator;

    @Autowired
    private JunchenJdOrderMapper orderMapper;

    @Override
    public PageResult<JunchenJdOrderDto> queryAll(JunchenJdOrderQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<JunchenJdOrder> page = new PageInfo<>(queryAll(criteria));
        PageResult<JunchenJdOrderDto> results = generator.convertPageInfo(page,JunchenJdOrderDto.class);
        results.getContent().stream().forEach(orderDto -> {
            if(!OrderUtil.JD_VALID_ORDER_STATUS.contains(orderDto.getValidCode())
                    || orderDto.getEstimateCosPrice()== null || orderDto.getEstimateCosPrice() <=0) {
                orderDto.setRemain("err");
            } else {
                orderDto.setRemain("ok");
            }
        });

        return results;
    }
    @Override
    //@Cacheable
    public List<JunchenJdOrder> queryAll(JunchenJdOrderQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(JunchenJdOrder.class, criteria));
    }


    @Override
    public void download(List<JunchenJdOrderDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JunchenJdOrderDto junchenJdOrder : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单号", junchenJdOrder.getOrderId());
            map.put("父单的订单号", junchenJdOrder.getParentId());
            map.put("下单时间", junchenJdOrder.getOrderTime());
            map.put("完成时间", junchenJdOrder.getFinishTime());
            map.put("更新时间", junchenJdOrder.getModifyTime());
            map.put("下单设备 1.pc 2.无线", junchenJdOrder.getOrderEmt());
            map.put("下单用户是否为PLUS会员 0：否，1：是", junchenJdOrder.getPlus());
            map.put("推客ID", junchenJdOrder.getUnionId());
            map.put("商品ID", junchenJdOrder.getSkuId());
            map.put("商品名称", junchenJdOrder.getSkuName());
            map.put("商品数量", junchenJdOrder.getSkuNum());
            map.put("商品已退货数量", junchenJdOrder.getSkuReturnNum());
            map.put("商品售后中数量", junchenJdOrder.getSkuFrozenNum());
            map.put("商品单价", junchenJdOrder.getPrice());
            map.put("佣金比例", junchenJdOrder.getCommissionRate());
            map.put("分成比例（单位：%）", junchenJdOrder.getSubsideRate());
            map.put("补贴比例（单位：%）", junchenJdOrder.getSubsidyRate());
            map.put("最终分佣比例", junchenJdOrder.getFinalRate());
            map.put("预估计订单金额", junchenJdOrder.getEstimateCosPrice());
            map.put("预估佣金，以这个为准", junchenJdOrder.getEstimateFee());
            map.put("实际订单金额", junchenJdOrder.getActualCosPrice());
            map.put("实际佣金，未结算时为0", junchenJdOrder.getActualFee());
            map.put("sku维度的有效码（-1：未知,2.无效-拆单,3.无效-取消,4.无效-京东帮帮主订单,5.无效-账号异常,6.无效-赠品类目不返佣,7.无效-校园订单,8.无效-企业订单,9.无效-团购订单,11.无效-乡村推广员下单,13.无效-违规订单,14.无效-来源与备案网址不符,15.待付款,16.已付款,17.已完成（购买用户确认收货）,20.无效-此复购订单对应的首购订单无效,21.无效-云店订单", junchenJdOrder.getValidCode());
            map.put("同跨店：2同店 3跨店", junchenJdOrder.getTraceType());
            map.put("推广位ID", junchenJdOrder.getPositionId());
            map.put("应用id（网站id、appid、社交媒体id）", junchenJdOrder.getSiteId());
            map.put("ID所属母账号平台名称", junchenJdOrder.getUnionAlias());
            map.put("格式:子推客ID_子站长应用ID_子推客推广位ID", junchenJdOrder.getPid());
            map.put("一级类目id", junchenJdOrder.getCid1());
            map.put("二级类目id", junchenJdOrder.getCid2());
            map.put("三级类目id", junchenJdOrder.getCid3());
            map.put("子渠道标识，在转链时可自定义传入，格式要求：字母、数字或下划线，最多支持80个字符", junchenJdOrder.getSubUnionId());
            map.put("联盟标签数据", junchenJdOrder.getUnionTag());
            map.put("商家ID", junchenJdOrder.getPopId());
            map.put("推客生成推广链接时传入的扩展字段", junchenJdOrder.getExt1());
            map.put("预估结算时间 yyyyMMdd", junchenJdOrder.getPayMonth());
            map.put("团长渠道ID", junchenJdOrder.getRid());
            map.put("微信用户json信息", junchenJdOrder.getGoodsInfo());
            map.put("创建时间", junchenJdOrder.getCreateTime());
            map.put("修改时间", junchenJdOrder.getUpdateTime());
            map.put("删除标识", junchenJdOrder.getIsDel());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public List<JunchenJdOrder> getUnbindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenJdOrder::getIsDel, false);
        wrapper.in(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);
        wrapper.isNull(JunchenJdOrder::getUid);

        LocalDateTime now = LocalDateTime.now().minusMinutes(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenJdOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.orderByDesc(JunchenJdOrder::getOrderTime);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public boolean hasUnlockOrder(Long uid, Integer innerType) {
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenJdOrder::getUid, uid);
        if(innerType != null) {
            wrapper.eq(JunchenJdOrder::getInnerType, innerType);
        }
        wrapper.eq(JunchenJdOrder::getBind, 0);
        wrapper.gt(JunchenJdOrder::getEstimateCosPrice, 0);
        wrapper.in(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);

        return orderMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Double totalFee(Integer type, List<Long> uid, Integer innerType) {
        return totalFee(type, uid, innerType, false);
    }

    @Override
    public List<JunchenJdOrder> getRefundAndBindOrderList(Integer prior) {
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenJdOrder::getIsDel, false);
        /*
        *
sku维度的有效码（-1：未知,2.无效-拆单,3.无效-取消,4.无效-京东帮帮主订单,5.无效-账号异常,
* 6.无效-赠品类目不返佣,7.无效-校园订单,8.无效-企业订单,9.无效-团购订单,11.无效-乡村推广员下单,
* 13.无效-违规订单,14.无效-来源与备案网址不符,15.待付款,
* 16.已付款,17.已完成（购买用户确认收货）,20.无效-此复购订单对应的首购订单无效,21.无效-云店订单
        * */
        wrapper.notIn(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);
        wrapper.eq(JunchenJdOrder::getBind, 1);

        LocalDateTime now = LocalDateTime.now().minusDays(prior);
        //查找prior秒之前的订单
        wrapper.ge(JunchenJdOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
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
    public Set<JunchenJdOrder> selectCashOrderIds(Long uid, LocalDateTime time) {
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


    private LambdaQueryWrapper<JunchenJdOrder> getWrapper(List<Long> uid, Integer innerType, DateTime start, boolean skipInvalid) {
        return getWrapperPro(uid, innerType, start, null, skipInvalid);
    }

    private LambdaQueryWrapper<JunchenJdOrder> getWrapperPro(List<Long> uid, Integer innerType, Date start, Date end, boolean skipInvalid) {
        LambdaQueryWrapper<JunchenJdOrder> wrapperTwo = new LambdaQueryWrapper<>();
        if(uid != null) {
            wrapperTwo.in(JunchenJdOrder::getUid, uid);
        }
        if(innerType != null) {
            wrapperTwo.eq(JunchenJdOrder::getInnerType, innerType);
        }
        if(start != null) {
            wrapperTwo.ge(JunchenJdOrder::getOrderTime, start);
        }
        if(end != null) {
            wrapperTwo.lt(JunchenJdOrder::getOrderTime, end);
        }
        if(skipInvalid) {
            wrapperTwo.in(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);
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
    public JunchenJdOrder mockOrder(Long orderId, Long skuId, Long uid, Date createTime, String shopName,
                                    Double rate,  Double fee, Double payPrice, String img, String title, Integer innerType) {
//        "{\"owner\": \"g\", " +
//                "\"shopId\": 1000094403, " +
//                "\"imageUrl\": \"http://img11.360buyimg.com/n0/jfs/t1/110776/40/5957/339361/5ea78ce7E48a4b3b5/6b317ccde37e7849.jpg\", " +
//                "\"shopName\": \"TCL环境电器京东自营旗舰店\", \"mainSkuId\": 6863099, \"productId\": 6863099}"
        GoodsInfoDto goodsInfo = GoodsInfoDto.builder().imageUrl(img).shopName(shopName).productId(orderId).build();

        JunchenJdOrder order = JunchenJdOrder.builder()
                .id(orderId.toString())
                .orderId(orderId)
                .parentId(0L)
                .orderTime(createTime)
                .modifyTime(createTime)
                .orderEmt(2)
                .plus(0)
                .unionId(1000482154L)
                .skuId(skuId)
                .skuName(title)
                .skuNum(1L)
                .skuReturnNum(0L)
                .skuFrozenNum(0L)
                .commissionRate(rate)
                .estimateFee(fee)
                .actualCosPrice(payPrice)
                .estimateCosPrice(payPrice)
                .validCode(16L)
                .positionId(3100121128L)
                .siteId(0L)
                .goodsInfo(goodsInfo)
                .build();
        order.setBind(0);
        order.setUid(uid);
        order.setInnerType(innerType);
        return order;
    }

    @Override
    public void invalidRefundOrders(List<Long> ids) {
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

    private LambdaQueryWrapper<JunchenJdOrder> getHbWrapper(Date start, Date end) {
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(JunchenJdOrder::getInnerType, 0);

        if(start != null) {
            wrapper.ge(JunchenJdOrder::getOrderTime, start);
        }
        if(end != null) {
            wrapper.lt(JunchenJdOrder::getOrderTime, end);
        }

        return wrapper;
    }


    @Override
    public List<JunchenJdOrder> getSelfUnspreadHbList(Integer limit) {
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenJdOrder::getIsDel, false);
        wrapper.eq(JunchenJdOrder::getBind, 0);
        wrapper.isNotNull(JunchenJdOrder::getUid);
        wrapper.eq(JunchenJdOrder::getInnerType, 0);
        wrapper.gt(JunchenJdOrder::getEstimateCosPrice, 0);
        wrapper.in(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);

        LocalDateTime now = LocalDateTime.now().minusDays(2);
        //查找2天之前的订单
        wrapper.lt(JunchenJdOrder::getOrderTime, Date.from(now.atZone( ZoneId.systemDefault()).toInstant()));
        wrapper.last("limit " + limit);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Long getSpreadCountToday(Long uid) {
        Date now = new Date();
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JunchenJdOrder::getUid, uid);
        wrapper.eq(JunchenJdOrder::getInnerType, 0);
        wrapper.le(JunchenJdOrder::getSpreadHbTime, DateUtil.endOfDay(now));
        wrapper.ge(JunchenJdOrder::getSpreadHbTime, DateUtil.beginOfDay(now));

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
        LambdaQueryWrapper<JunchenJdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(JunchenJdOrder::getUid);
        wrapper.eq(JunchenJdOrder::getInnerType, 0);
        wrapper.in(JunchenJdOrder::getValidCode, JD_VALID_ORDER_STATUS);
        wrapper.gt(JunchenJdOrder::getCommissionRate, scale);

        Date yesterdayStart = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -prior));
        wrapper.ge(JunchenJdOrder::getOrderTime, yesterdayStart);
        wrapper.groupBy(JunchenJdOrder::getUid);
        return orderMapper.checkCount(wrapper);
    }
}
