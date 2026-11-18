/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tools.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.tools.domain.QiniuConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-13
*/
@Repository
@Mapper
public interface QiniuConfigMapper extends CoreMapper<QiniuConfig> {


    @Update("update system_qiniu_config set type = #{type} ")
    void updateType(@Param("type") String type);
}
