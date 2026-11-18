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
public class MwSystemStoreStaffQueryCriteria {

    /** 模糊 */
    @Query(type = Query.Type.INNER_LIKE)
    private String staffName;

    /** 模糊 */
    @Query(type = Query.Type.INNER_LIKE)
    private String nickname;
}
