/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

import java.util.Set;

/**
* @author shenji
* @date 2022-08-29
*/
@Data
public class JunchenOrderQueryCriteria {
    @Query(type = Query.Type.EQUAL)
    private Long uid;
    @Query(type = Query.Type.EQUAL)
    private Integer innerType;

    @Query
    private Integer refundTag;

    @Query(propName = "uid",  type = Query.Type.IN)
    private Set<Long> uids;
    /**0=自己 1=金客 2=银客*/
    private Integer level;

    @Query(type = Query.Type.EQUAL)
    private Integer bind;

}
