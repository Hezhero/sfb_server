/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.system.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.modules.system.domain.RolesDepts;
import com.junchen.modules.system.service.RolesDeptsService;
import com.junchen.modules.system.service.mapper.RolesDeptsMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
* @author mazhongjun
* @date 2020-05-16
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "rolesDepts")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RolesDeptsServiceImpl extends BaseServiceImpl<RolesDeptsMapper, RolesDepts> implements RolesDeptsService {

}
