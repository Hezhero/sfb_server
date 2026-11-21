/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.dict.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

/**
* @author huangyu
* @date 2020-05-14
*/
@Data
public class DictQueryCriteria{

    @Query(blurry = "biz_name,remark")
    private String blurry;
}
