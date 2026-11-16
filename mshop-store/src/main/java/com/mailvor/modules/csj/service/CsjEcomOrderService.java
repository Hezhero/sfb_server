/**
 * Copyright (C) 2018-2026
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.csj.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mailvor.modules.tk.domain.MailvorDyOrder;
import com.mailvor.modules.tk.service.MailvorDyOrderService;
import com.mailvor.utils.OrderUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 穿山甲电商联盟订单同步
 *
 * <p>把穿山甲订单写入 {@code mw_order_dy}，与现有抖音订单共用一张表、一套分佣逻辑、
 * 一个后台页面和 APP 订单列表。</p>
 *
 * <p>字段映射（依官方文档 3.4 订单接口）：</p>
 * <table>
 *   <tr><th>穿山甲</th><th>mw_order_dy</th><th>转换</th></tr>
 *   <tr><td>order_id</td><td>order_id</td><td>—</td></tr>
 *   <tr><td>product_id / product_name</td><td>product_id / product_name</td><td>—</td></tr>
 *   <tr><td>total_pay_amount</td><td>total_pay_amount</td><td>分 → 元</td></tr>
 *   <tr><td>pay_goods_amount</td><td>pay_goods_amount</td><td>分 → 元</td></tr>
 *   <tr><td>estimated_commission</td><td>estimated_commission</td><td>分 → 元</td></tr>
 *   <tr><td>ads_real_commission</td><td>real_commission</td><td>分 → 元</td></tr>
 *   <tr><td>split_rate（万分之一）</td><td>commission_rate（百分比）</td><td>÷ 100</td></tr>
 *   <tr><td>flow_point</td><td>flow_point</td><td>值域一致</td></tr>
 *   <tr><td>external_info</td><td>external_info</td><td>用于反解 uid</td></tr>
 * </table>
 *
 * <p><b>幂等</b>：实体没有 id 字段，不能直接用 saveOrUpdateBatch（会重复插入）。
 * 这里按 order_id 判断：不存在则 insert，存在则只更新状态类字段，避免覆盖用户已产生的
 * bind / hb 等业务数据。</p>
 *
 * @author mailvor
 */
@Slf4j
@Service
@AllArgsConstructor
public class CsjEcomOrderService {

    /** 官方限制：size 取值 [1,50] */
    private static final int PAGE_SIZE = 50;
    /** 防死循环兜底：40 页 × 50 条 = 2000 条/次 */
    private static final int MAX_PAGE = 40;
    /** 官方限制：时间跨度不超过 90 天 */
    private static final long MAX_SPAN_SECONDS = 90L * 24 * 3600;

    private final CsjEcomClient client;
    private final MailvorDyOrderService dyOrderService;

    /**
     * 同步指定时间窗的订单（游标分页拉全量）
     *
     * @return 新增 + 更新的条数
     */
    public int syncOrders(Date start, Date end) {
        long startSec = start.getTime() / 1000;
        long endSec = end.getTime() / 1000;
        if (startSec > endSec) {
            long tmp = startSec;
            startSec = endSec;
            endSec = tmp;
        }
        if (endSec - startSec > MAX_SPAN_SECONDS) {
            startSec = endSec - MAX_SPAN_SECONDS;
        }

        String cursor = "0";
        int affected = 0;
        int page = 0;

        while (page++ < MAX_PAGE) {
            JSONObject data;
            try {
                data = client.orderSearchPage(cursor, PAGE_SIZE, startSec, endSec,
                        CsjEcomClient.ORDER_TYPE_PRODUCT, "pay");
            } catch (Exception e) {
                log.error("穿山甲订单拉取失败 cursor={} err={}", cursor, e.getMessage());
                break;
            }
            if (data == null || data.isEmpty()) {
                break;
            }
            JSONArray orders = data.getJSONArray("orders");
            if (orders != null) {
                for (int i = 0; i < orders.size(); i++) {
                    JSONObject item = orders.getJSONObject(i);
                    try {
                        if (saveOrUpdateOne(item)) {
                            affected++;
                        }
                    } catch (Exception e) {
                        log.warn("穿山甲订单写入失败 orderId={} err={}",
                                item.getString("order_id"), e.getMessage());
                    }
                }
            }
            // 官方约定：cursor 返回 "" 表示没有更多
            String next = data.getString("cursor");
            if (StrUtil.isBlank(next)) {
                break;
            }
            cursor = next;
        }

        log.info("穿山甲订单同步完成 {} ~ {} 共处理 {} 条",
                DateUtil.formatDateTime(start), DateUtil.formatDateTime(end), affected);
        return affected;
    }

    private boolean saveOrUpdateOne(JSONObject j) {
        String orderId = j.getString("order_id");
        if (StrUtil.isBlank(orderId)) {
            return false;
        }

        String externalInfo = j.getString("external_info");
        String flowPoint = j.getString("flow_point");
        Double totalPay = fen2yuan(j.get("total_pay_amount"));
        Double payGoods = fen2yuan(j.get("pay_goods_amount"));
        Double estimatedCommission = fen2yuan(j.get("estimated_commission"));
        Double realCommission = fen2yuan(j.get("ads_real_commission"));
        Double techFee = fen2yuan(j.get("estimated_tech_service_fee"));
        // split_rate 单位为万分之一，10 表示 0.10%，换算成百分比需 ÷100
        Double splitRate = j.getDouble("split_rate");
        Double commissionRate = splitRate == null ? null : splitRate / 100.0d;
        Date payTime = parseTime(j.getString("pay_success_time"));
        Date settleTime = parseTime(j.getString("settle_time"));
        Date refundTime = parseTime(j.getString("refund_time"));

        Long count = dyOrderService.count(Wrappers.<MailvorDyOrder>lambdaQuery()
                .eq(MailvorDyOrder::getOrderId, orderId));
        boolean exists = count != null && count > 0;

        if (!exists) {
            MailvorDyOrder order = new MailvorDyOrder();
            order.setOrderId(orderId);
            order.setProductId(j.getString("product_id"));
            order.setProductName(j.getString("product_name"));
            order.setProductImg(j.getString("product_img"));
            order.setItemNum(j.getInteger("item_num"));
            order.setTotalPayAmount(totalPay);
            order.setPayGoodsAmount(payGoods);
            order.setEstimatedCommission(estimatedCommission);
            order.setRealCommission(realCommission);
            order.setEstimatedTechServiceFee(techFee);
            order.setCommissionRate(commissionRate);
            order.setFlowPoint(flowPoint);
            order.setExternalInfo(externalInfo);
            order.setPaySuccessTime(payTime);
            order.setSettleTime(settleTime);
            order.setRefundTime(refundTime);
            order.setShopId(j.getInteger("shop_id"));
            order.setShopName(j.getString("shop_name"));
            order.setMediaTypeName(j.getString("media_type_name"));
            order.setInnerType(0);

            Long uid = OrderUtil.getDyOrderUser(externalInfo);
            if (uid != null && uid > 0) {
                order.setUid(uid);
            }
            dyOrderService.save(order);
            return true;
        }

        // 已存在：只更新状态与佣金，不碰 uid / bind / hb（用户已产生的数据）
        dyOrderService.update(Wrappers.<MailvorDyOrder>lambdaUpdate()
                .eq(MailvorDyOrder::getOrderId, orderId)
                .set(flowPoint != null, MailvorDyOrder::getFlowPoint, flowPoint)
                .set(settleTime != null, MailvorDyOrder::getSettleTime, settleTime)
                .set(refundTime != null, MailvorDyOrder::getRefundTime, refundTime)
                .set(realCommission != null, MailvorDyOrder::getRealCommission, realCommission)
                .set(estimatedCommission != null,
                        MailvorDyOrder::getEstimatedCommission, estimatedCommission));
        return true;
    }

    /** 分 → 元 */
    private Double fen2yuan(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value)) / 100.0d;
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return DateUtil.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
