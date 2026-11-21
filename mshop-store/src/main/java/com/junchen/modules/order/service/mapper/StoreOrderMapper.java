/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.order.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.order.domain.MwStoreOrder;
import com.junchen.modules.order.service.dto.ChartDataDto;
import com.junchen.modules.order.vo.OrderDataVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface StoreOrderMapper extends CoreMapper<MwStoreOrder> {



    @Select("SELECT sum(pay_integral) as price,count(id) as count," +
            "to_char(create_time, 'MM-DD') as time FROM mw_store_order" +
            " WHERE is_del = false " +
            "GROUP BY to_char(create_time,'YYYY-MM-DD'), to_char(create_time, 'MM-DD') " +
            "ORDER BY to_char(create_time, 'YYYY-MM-DD') DESC")
    List<OrderDataVo> getOrderDataPriceList(Page page);


    @Select("SELECT COALESCE(sum(pay_integral),0) " +
            " FROM mw_store_order ${ew.customSqlSegment}")
    Double todayPrice(@Param(Constants.WRAPPER) Wrapper<MwStoreOrder> wrapper);



    @Select("select COALESCE(sum(pay_integral),0) from mw_store_order " +
            "where is_del=false and uid=#{uid}")
    double sumPrice(@Param("uid") Long uid);


    @Select("SELECT COUNT(*) FROM mw_store_order WHERE pay_time >= ${today}")
    Integer countByPayTimeGreaterThanEqual(@Param("today")int today);

    @Select("SELECT COUNT(*) FROM mw_store_order WHERE pay_time < ${today}  and pay_time >= ${yesterday}")
    Integer countByPayTimeLessThanAndPayTimeGreaterThanEqual(@Param("today")int today, @Param("yesterday")int yesterday);

    @Select( "select COALESCE(sum(pay_integral),0)  from mw_store_order " +
            "where is_del=false")
    Double sumTotalPrice();

    @Select("SELECT COALESCE(sum(pay_integral),0) as num," +
            "to_char(create_time, 'MM-DD') as time " +
            " FROM mw_store_order where is_del=false and create_time >= #{time}" +
            " GROUP BY to_char(create_time,'YYYY-MM-DD'), to_char(create_time, 'MM-DD') " +
            " ORDER BY to_char(create_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartList(@Param("time") Date time);
    @Select("SELECT count(id) as num," +
            "to_char(create_time, 'MM-DD') as time " +
            " FROM mw_store_order where is_del=false and create_time >= #{time}" +
            " GROUP BY to_char(create_time,'YYYY-MM-DD'), to_char(create_time, 'MM-DD') " +
            " ORDER BY to_char(create_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartListT(@Param("time") Date time);
}
