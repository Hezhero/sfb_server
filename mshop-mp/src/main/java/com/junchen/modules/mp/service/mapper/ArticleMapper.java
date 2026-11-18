/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.mp.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.mp.domain.MwArticle;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface ArticleMapper extends CoreMapper<MwArticle> {
    @Update("update mw_article set visit=visit+1 " +
            "where id=#{id}")
    int incVisitNum(@Param("id") int id);

}
