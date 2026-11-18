/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.product.domain.MwStoreProductAttrResult;

import java.util.Map;


/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwStoreProductAttrResultService extends BaseService<MwStoreProductAttrResult>{

    /**
     * 新增商品属性详情
     * @param map map
     * @param productId 商品id
     */
    void insertMwStoreProductAttrResult(Map<String, Object> map, Long productId);
}
