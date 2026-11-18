/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.activity.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.activity.domain.MwStoreVisit;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-13
*/
@Repository
public interface MwStoreVisitMapper extends CoreMapper<MwStoreVisit> {

    /**
     * 拼团浏览量
     * @param productId
     * @return
     */
    @Update("update mw_store_visit set count=count+1 " +
            "where uid=#{uid} AND id=#{productId}")
    int incBrowseNum(@Param("uid") Long uid,@Param("productId") Long productId);
}
