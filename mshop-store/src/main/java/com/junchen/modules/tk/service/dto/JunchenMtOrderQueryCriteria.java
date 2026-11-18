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
* @date 2022-08-29
*/
@Data
public class JunchenMtOrderQueryCriteria extends JunchenOrderQueryCriteria{
    @Query(type = Query.Type.EQUAL)
    private Integer itemStatus;

    @Query(type = Query.Type.EQUAL)
    private Integer itemBizStatus;

    @Query(blurry = "uniqueItemId,orderId,uid")
    private String value;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> orderPayTime;
}
