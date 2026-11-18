/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tools.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.tools.domain.AlipayConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-13
*/
@Repository
@Mapper
public interface AlipayConfigMapper extends CoreMapper<AlipayConfig> {

}
