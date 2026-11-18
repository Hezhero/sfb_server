/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

/**
* @author huangyu
* @date 2020-05-12
*/
@Data
public class MwUserCardQueryCriteria {

    // 模糊
    @Query(type = Query.Type.INNER_LIKE)
    private String cardName;

    @Query(blurry = "phone,card_no")
    private String blurry;

    @Query(blurry = "phone,card_no_enc")
    private String blurryEnc;

    @Query(type = Query.Type.EQUAL)
    private Long uid;

}
