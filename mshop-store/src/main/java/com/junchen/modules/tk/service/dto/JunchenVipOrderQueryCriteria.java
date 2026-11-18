/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
* @author wangjun
* @date 2022-09-07
*/
@Data
public class JunchenVipOrderQueryCriteria extends JunchenOrderQueryCriteria{
    @Query
    private String orderSubStatusName;

    @Query(blurry = "orderSn,detailList,uid")
    private String value;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> orderTime;
}
