/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service.dto;

import lombok.Data;
import com.junchen.annotation.Query;

/**
 * @author huangyu
 * @date 2020-09-03
 */
@Data
public class MwStoreProductRelationQueryCriteria {
    @Query(type = Query.Type.EQUAL, propName = "biz_type")
    private String type;
}
