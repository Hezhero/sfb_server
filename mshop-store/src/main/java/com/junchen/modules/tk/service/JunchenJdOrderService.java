/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import com.junchen.domain.PageResult;
import com.junchen.modules.tk.domain.JunchenJdOrder;
import com.junchen.modules.tk.service.dto.JunchenJdOrderDto;
import com.junchen.modules.tk.service.dto.JunchenJdOrderQueryCriteria;
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
* @date 2022-09-05
*/
public interface JunchenJdOrderService  extends TkOrderService<JunchenJdOrder> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<JunchenJdOrderDto> queryAll(JunchenJdOrderQueryCriteria criteria, Pageable pageable);
    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<JunchenJdOrderDto>
    */
    List<JunchenJdOrder> queryAll(JunchenJdOrderQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<JunchenJdOrderDto> all, HttpServletResponse response) throws IOException;

    List<JunchenJdOrder> getUnbindOrderList(Integer prior);
    List<JunchenJdOrder> getRefundAndBindOrderList(Integer prior);

    void bindUser(Long uid, Long id);
    void refundOrder(Long id);


    Set<JunchenJdOrder> selectCashOrderIds(Long uid, LocalDateTime time);


    JunchenJdOrder mockOrder(Long orderId,  Long skuId, Long uid, Date createTime, String shopName,
                             Double rate,  Double fee, Double payPrice, String img, String title, Integer innerType);

    void invalidRefundOrders(List<Long> ids);

    List<JunchenJdOrder> getSelfUnspreadHbList(Integer limit);


    Long getSpreadCountToday(Long uid);
    List<OrderCheckDTO> checkCount(Integer prior, Double scale);

}
