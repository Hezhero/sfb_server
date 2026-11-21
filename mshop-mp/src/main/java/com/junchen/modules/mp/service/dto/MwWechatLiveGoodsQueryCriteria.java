/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.mp.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

/**
* @author huangyu
* @date 2020-08-11
*/
@Data
public class MwWechatLiveGoodsQueryCriteria {

    /** 模糊 */
    @Query(type = Query.Type.INNER_LIKE, propName = "biz_name")
    private String name;
}
