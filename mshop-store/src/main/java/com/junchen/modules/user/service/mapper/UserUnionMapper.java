/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.user.domain.MwUserUnion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2023-05-12
*/
@Repository
public interface UserUnionMapper extends CoreMapper<MwUserUnion> {
    @Update("update mw_user_union set tlj_count=tlj_count+1" +
            " where uid=#{uid}")
    int incTljCount(@Param("uid") Long uid);


}
