/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.shop.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

/**
* @author huangyu
* @date 2020-05-12
*/
@Data
public class MwSystemGroupDataQueryCriteria {

    // 精确
    @Query
    private String groupName;

    @Query
    private Integer status;
}
