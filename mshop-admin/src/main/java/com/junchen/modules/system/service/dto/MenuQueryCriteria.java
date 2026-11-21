/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.system.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
* @author huangyu
* @date 2020-05-14
*/
@Data
public class MenuQueryCriteria{

    @Query(blurry = "biz_name")
    private String blurry;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
