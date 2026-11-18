/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.user.domain.MwUserCard;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface UserCardMapper extends CoreMapper<MwUserCard> {

    @Update( "update mw_user_card set contract_path = null,signature_path=null where uid = #{id}")
    void clearSignature(@Param("id") Long id);

}
