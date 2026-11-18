/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import com.junchen.domain.PageResult;
import com.junchen.modules.tk.domain.JunchenPddOrder;
import com.junchen.modules.tk.service.dto.JunchenPddOrderDto;
import com.junchen.modules.tk.service.dto.JunchenPddOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
* @author shenji
* @date 2022-09-06
*/
public interface JunchenPddOrderService  extends TkOrderService<JunchenPddOrder> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<JunchenPddOrderDto> queryAll(JunchenPddOrderQueryCriteria criteria, Pageable pageable);
    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<JunchenPddOrderDto>
    */
    List<JunchenPddOrder> queryAll(JunchenPddOrderQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<JunchenPddOrderDto> all, HttpServletResponse response) throws IOException;

    List<JunchenPddOrder> getUnbindOrderList(Integer prior);
    List<JunchenPddOrder> getRefundAndBindOrderList(Integer prior);
    void bindUser(Long uid, String orderSn);
    void refundOrder(String orderSn);



    Set<JunchenPddOrder> selectCashOrderIds(Long uid, LocalDateTime time);


    JunchenPddOrder mockOrder(String orderId, String goodsSign, Long uid, Date createTime, String shopName,
                              Double rate, Double fee, Double payPrice, String img, String title, Integer innerType);

    void invalidRefundOrders(List<String> ids);

    List<JunchenPddOrder> getSelfUnspreadHbList(Integer limit);


    Long getSpreadCountToday(Long uid);
    List<OrderCheckDTO> checkCount(Integer prior, Double scale);
}
