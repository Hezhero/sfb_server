/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.modules.product.domain.MwStoreProductAttrResult;
import com.junchen.modules.product.service.MwStoreProductAttrResultService;
import com.junchen.modules.product.service.mapper.StoreProductAttrResultMapper;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;


/**
* @author huangyu
* @date 2020-05-12
*/
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwStoreProductAttrResultServiceImpl extends BaseServiceImpl<StoreProductAttrResultMapper, MwStoreProductAttrResult> implements MwStoreProductAttrResultService {

    /**
     * 新增商品属性详情
     * @param map map
     * @param productId 商品id
     */
    @Override
    public void insertMwStoreProductAttrResult(Map<String, Object> map, Long productId)
    {
        MwStoreProductAttrResult mwStoreProductAttrResult = new MwStoreProductAttrResult();
        mwStoreProductAttrResult.setProductId(productId);
        mwStoreProductAttrResult.setResult(JSON.toJSONString(map));
        mwStoreProductAttrResult.setChangeTime(new Date());

        Long count = this.count(Wrappers.<MwStoreProductAttrResult>lambdaQuery()
                .eq(MwStoreProductAttrResult::getProductId,productId));
        if(count > 0) {
            this.remove(Wrappers.<MwStoreProductAttrResult>lambdaQuery()
                    .eq(MwStoreProductAttrResult::getProductId,productId));
        }

        this.save(mwStoreProductAttrResult);
    }


}
