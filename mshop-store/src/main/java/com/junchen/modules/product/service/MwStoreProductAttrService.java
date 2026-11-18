/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.product.domain.MwStoreProductAttr;
import com.junchen.modules.product.domain.MwStoreProductAttrValue;
import com.junchen.modules.product.service.dto.FromatDetailDto;
import com.junchen.modules.product.service.dto.ProductFormatDto;

import java.util.List;
import java.util.Map;


/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwStoreProductAttrService extends BaseService<MwStoreProductAttr>{

    /**
     * 增加库存减去销量
     * @param num 数量
     * @param productId 商品id
     * @param unique sku唯一值
     */
    void incProductAttrStock(Integer num, Long productId, String unique,String type);

    /**
     * 减少库存增加销量
     * @param num 数量
     * @param productId 商品id
     */
    void decProductAttrStock(int num, Long productId, String type);


    /**
     * 更加sku 唯一值获取sku对象
     * @param unique 唯一值
     * @return MwStoreProductAttrValue
     */
    MwStoreProductAttrValue uniqueByAttrInfo(String unique);

    /**
     * 获取商品sku属性
     * @param productId 商品id
     * @return map
     */
    Map<String, Object> getProductAttrDetail(long productId);

    /**
     * 新增商品属性
     * @param items attr
     * @param attrs value
     * @param productId 商品id
     */
    void insertMwStoreProductAttr(List<FromatDetailDto> items, List<ProductFormatDto> attrs,
                                  Long productId);
}
