/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import com.junchen.domain.PageResult;
import com.junchen.modules.tk.domain.JunchenMtOrder;
import com.junchen.modules.tk.service.dto.JunchenMtOrderQueryCriteria;
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
* @date 2022-08-29
*/
public interface JunchenMtOrderService extends TkOrderService<JunchenMtOrder> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<JunchenMtOrder> queryAll(JunchenMtOrderQueryCriteria criteria, Pageable pageable);
    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<JunchenMtOrder>
    */
    List<JunchenMtOrder> queryAll(JunchenMtOrderQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<JunchenMtOrder> all, HttpServletResponse response) throws IOException;


    List<JunchenMtOrder> getUnbindOrderList(Integer prior);

    List<JunchenMtOrder> getRefundAndBindOrderList(Integer prior);

    Set<JunchenMtOrder> selectCashOrderIds(Long uid, LocalDateTime time);

    JunchenMtOrder mockOrder(Long orderId, Long uid, Date createTime, String shopName,
                             Double rate, Double fee,  Double payPrice,String img,String title,
                             String link, String itemId, Integer innerType);
    void bindUser(Long uid, Long id);
    void refundOrder(Long id);

    void invalidRefundOrders(List<Long> ids);

    List<JunchenMtOrder> getSelfUnspreadHbList(Integer day, Integer limit);

    Long getSpreadCountToday(Long uid);

    List<OrderCheckDTO> checkCount(Integer prior, Double scale);
}
