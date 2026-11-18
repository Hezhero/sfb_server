/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.order.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.order.domain.MwStoreOrder;
import com.junchen.modules.order.service.dto.UserRefundDto;
import com.junchen.modules.shop.domain.MwSystemUserLevel;
import com.junchen.modules.tk.domain.*;
import com.junchen.modules.user.config.HbUnlockConfig;
import com.junchen.modules.user.domain.MwUser;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
* @author huangyu
* @date 2020-05-12
*/
public interface SuStoreOrderService extends BaseService<MwStoreOrder>{

    /**
     * 返回订单确认数据
     * @param mwUser mwUser
     * @param goodsId id
     * @return ConfirmOrderVO
     */
    boolean confirmIntegral(MwUser mwUser, Long goodsId);

//    void gainUserIntegral(Long uid, String orderId, Date orderCreateTime, BigDecimal gainIntegral, PlatformEnum platformEnum);

    BigDecimal calIntegral(MwSystemUserLevel userLevel, double preFee);

    void bindOrder(Long uid, JunchenTbOrder order);
    void decHbAndRefundOrder(Long uid, JunchenTbOrder order);

    void bindOrder(Long uid, JunchenJdOrder order);
    void decHbAndRefundOrder(Long uid, JunchenJdOrder order);

    void bindOrder(Long uid, JunchenPddOrder order);
    void decHbAndRefundOrder(Long uid, JunchenPddOrder order);

    void bindOrder(Long uid, JunchenVipOrder order);
    void decHbAndRefundOrder(Long uid, JunchenVipOrder order);

    void bindOrder(Long uid, JunchenDyOrder order);
    void decHbAndRefundOrder(Long uid, JunchenDyOrder order);
    void decHbAndRefundOrder(Long uid, JunchenMtOrder order);
    Map<String, Double> incMoneyAndBindOrder(Long uid, TkOrder order, Date unlockTime);

    Date checkOrder(TkOrder tkOrder, Long uid);

    Date checkSelfOrder(TkOrder tkOrder, UserRefundDto user, HbUnlockConfig unlockConfig);

    void bindOrder(Long uid, JunchenMtOrder order);
}
