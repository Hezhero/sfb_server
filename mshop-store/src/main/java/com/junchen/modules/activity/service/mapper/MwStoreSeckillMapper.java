/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.activity.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.activity.domain.MwStoreSeckill;
import com.junchen.modules.product.vo.MwStoreProductQueryVo;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
* @author huangyu
* @date 2020-05-13
*/
@Repository
public interface MwStoreSeckillMapper extends CoreMapper<MwStoreSeckill> {

    @Select("SELECT c.id,c.image,c.price,c.title as storeName,c.is_show as isShow,c.biz_cost as cost," +
            "c.is_postage as isPostage,c.postage,c.sales,c.stock,c.is_del as isDel" +
            " FROM mw_store_seckill c " +
            " WHERE c.id = #{id} and c.is_del = false ")
    MwStoreProductQueryVo seckillInfo(Long id);
}
