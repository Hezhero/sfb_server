/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.cart.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.cart.domain.MwStoreCart;
import com.junchen.modules.order.service.dto.CountDto;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface StoreCartMapper extends CoreMapper<MwStoreCart> {

    @Select("select COALESCE(sum(cart_num),0) from mw_store_cart " +
            "where is_pay=false and is_del=false and is_new=false and uid=#{uid}")
    int cartSum(@Param("uid") Long uid);


    @Select("SELECT t.cate_name as catename from mw_store_cart c  " +
            "LEFT JOIN mw_store_product p on c.product_id = p.id  " +
            "LEFT JOIN mw_store_category t on p.cate_id = t.id " +
            "WHERE c.is_pay = true")
    List<CountDto> findCateName();
}
