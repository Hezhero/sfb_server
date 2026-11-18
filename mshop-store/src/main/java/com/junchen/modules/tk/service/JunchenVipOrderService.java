/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service;

import com.junchen.domain.PageResult;
import com.junchen.modules.tk.domain.JunchenVipOrder;
import com.junchen.modules.tk.service.dto.JunchenVipOrderDto;
import com.junchen.modules.tk.service.dto.JunchenVipOrderQueryCriteria;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
* @author wangjun
* @date 2022-09-07
*/
public interface JunchenVipOrderService  extends TkOrderService<JunchenVipOrder> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<JunchenVipOrderDto> queryAll(JunchenVipOrderQueryCriteria criteria, Pageable pageable);
    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<JunchenVipOrderDto>
    */
    List<JunchenVipOrder> queryAll(JunchenVipOrderQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<JunchenVipOrderDto> all, HttpServletResponse response) throws IOException;

    List<JunchenVipOrder> getUnbindOrderList(Long prior);
    List<JunchenVipOrder> getRefundAndBindOrderList(Integer prior);
    void bindUser(Long uid, String orderSn);
    void refundOrder(String id);

    Set<JunchenVipOrder> selectCashOrderIds(Long uid, LocalDateTime time);

    JunchenVipOrder mockOrder(String orderId, Long uid, Date createTime, String shopName,
                              Double rate, Double fee, Double payPrice, String img, String title,
                              String itemId, Integer innerType);

    void invalidRefundOrders(List<String> ids);

    List<JunchenVipOrder> getSelfUnspreadHbList(Integer limit);

    Long getSpreadCountToday(Long uid);
    List<OrderCheckDTO> checkCount(Integer prior, Double scale);

}
