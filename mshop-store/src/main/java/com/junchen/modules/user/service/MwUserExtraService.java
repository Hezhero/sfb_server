/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserExtra;
import com.junchen.modules.user.service.dto.MwUserQueryCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwUserExtraService extends BaseService<MwUserExtra>{

    Map<String,Object> queryAll(MwUserQueryCriteria criteria, Pageable pageable);

    List<MwUserExtra> queryAll(MwUserQueryCriteria criteria);

}
