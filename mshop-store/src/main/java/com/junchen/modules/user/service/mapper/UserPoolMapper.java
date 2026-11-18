/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.user.domain.MwUserPool;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface UserPoolMapper extends CoreMapper<MwUserPool> {

    @Select("SELECT refund FROM mw_user_pool WHERE uid = #{uid}")
    Integer getRefund(Long uid);

    @Update("update mw_user_pool set refund=refund+1 WHERE uid = #{uid}")
    Integer addRefund(Long uid);

    @Update("update mw_user_pool set refund=0 WHERE uid = #{uid}")
    Integer resetRefund(Long uid);
}
