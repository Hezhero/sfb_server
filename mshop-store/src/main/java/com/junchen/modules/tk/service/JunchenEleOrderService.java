/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import com.junchen.domain.PageResult;
import com.junchen.modules.tk.domain.JunchenEleOrder;
import com.junchen.modules.tk.service.dto.JunchenEleOrderDto;
import com.junchen.modules.tk.service.dto.JunchenEleOrderQueryCriteria;
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
* @date 2022-09-07
*/
public interface JunchenEleOrderService extends TkOrderService<JunchenEleOrder> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<JunchenEleOrderDto> queryAll(JunchenEleOrderQueryCriteria criteria, Pageable pageable);
    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<JunchenEleOrderDto>
    */
    List<JunchenEleOrder> queryAll(JunchenEleOrderQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<JunchenEleOrderDto> all, HttpServletResponse response) throws IOException;

    List<JunchenEleOrder> getUnbindOrderList(Integer prior);
    List<JunchenEleOrder> getRefundAndBindOrderList(Integer prior);
    void bindUser(Long uid, String orderId);
    void unbindUser(String id);


    Set<JunchenEleOrder> selectCashOrderIds(Long uid, LocalDateTime time);


    JunchenEleOrder mockOrder(String orderId, String productId, Long uid, Date createTime, String shopName,
                             Double rate, Double fee, Double payPrice, String img, String title, Integer innerType);

    void invalidRefundOrders(List<String> ids);

    List<JunchenEleOrder> getSelfUnspreadHbList(Integer day, Integer limit);


    Long getSpreadCountToday(Long uid);
    List<OrderCheckDTO> checkCount(Integer prior, Double scale);
}
