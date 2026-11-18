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
* @author shenji
* @date 2022-09-06
*/
@Data
public class JunchenPddOrderQueryCriteria extends JunchenOrderQueryCriteria{
    @Query
    private Integer orderStatus;

    @Query(blurry = "orderSn,goodsName,goodsSign,uid")
    private String value;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> orderCreateTime;
}
