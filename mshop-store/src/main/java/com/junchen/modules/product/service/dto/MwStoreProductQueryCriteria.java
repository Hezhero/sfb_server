/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

/**
* @author huangyu
* @date 2020-05-12
*/
@Data
public class MwStoreProductQueryCriteria {

    // 模糊
    @Query(type = Query.Type.INNER_LIKE)
    private String storeName;

    // 精确
    @Query
    private Boolean isDel;

    @Query
    private Integer isShow;

    @Query
    private Integer cateId;

    @Query
    private Integer isIntegral;
}
