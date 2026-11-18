/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.modules.product.domain.MwStoreProductAttrValue;
import com.junchen.modules.product.service.MwStoreProductAttrValueService;
import com.junchen.modules.product.service.mapper.StoreProductAttrValueMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
* @author huangyu
* @date 2020-05-12
*/
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwStoreProductAttrValueServiceImpl extends BaseServiceImpl<StoreProductAttrValueMapper, MwStoreProductAttrValue> implements MwStoreProductAttrValueService {


}
