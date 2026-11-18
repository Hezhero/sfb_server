/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.system.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.modules.system.domain.RolesMenus;
import com.junchen.modules.system.service.RolesMenusService;
import com.junchen.modules.system.service.mapper.RolesMenusMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
* @author huangyu
* @date 2020-05-16
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "rolesMenus")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RolesMenusServiceImpl extends BaseServiceImpl<RolesMenusMapper, RolesMenus> implements RolesMenusService {

}
