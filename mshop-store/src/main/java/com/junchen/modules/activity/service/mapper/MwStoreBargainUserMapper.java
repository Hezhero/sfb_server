/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.activity.service.mapper;


import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.activity.domain.MwStoreBargainUser;
import com.junchen.modules.activity.vo.MwStoreBargainUserQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 用户参与砍价表 Mapper 接口
 * </p>
 *
 * @author mazhongjun
 * @since 2019-12-21
 */
@Repository
public interface MwStoreBargainUserMapper extends CoreMapper<MwStoreBargainUser> {


    @Select("SELECT u.uid,u.is_del as isDel,u.bargain_price - u.price as residuePrice,u.id," +
            "u.bargain_id as bargainId,u.bargain_price as bargainPrice," +
            "u.bargain_price_min as bargainPriceMin,u.price,u.status,b.title," +
            "b.image,b.stop_time as datatime FROM mw_store_bargain_user u INNER JOIN " +
            "mw_store_bargain b ON b.id=u.bargain_id WHERE u.uid = #{uid} AND u.is_del = false " +
            "ORDER BY u.id DESC ")
    List<MwStoreBargainUserQueryVo> getBargainUserList(@Param("uid") Long uid, Page page);


}
