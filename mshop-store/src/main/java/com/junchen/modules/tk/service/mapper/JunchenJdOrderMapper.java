/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.order.service.dto.ChartDataDto;
import com.junchen.modules.tk.domain.JunchenJdOrder;
import com.junchen.modules.tk.service.dto.OrderCheckDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
* @author shenji
* @date 2022-09-05
*/
@Repository
public interface JunchenJdOrderMapper extends CoreMapper<JunchenJdOrder> {

    @Override
    @Select("SELECT * FROM mw_order_jd ${ew.customSqlSegment}")
    <E extends IPage<JunchenJdOrder>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<JunchenJdOrder> queryWrapper);

    @Update("update mw_order_jd set uid = ${uid} where order_id = #{id}")
    void bindUser(@Param("uid")Long uid, @Param("id") Long id);
    @Update("update mw_order_jd set bind = 2 where order_id = #{id}")
    void refundOrder(@Param("id") Long id);


    @Select("select IFNULL(sum(integral),0) from mw_order_jd " +
            "where uid=#{uid} and order_time <= #{time} and bind=1 and cash=0")
    double totalCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select("select order_id from mw_order_jd where uid=#{uid} and order_time <= #{time} and cash=0")
    Set<JunchenJdOrder> selectCashOrderId(@Param("uid") Long uid, @Param("time") Date time);

    @Update("update mw_order_jd set cash = 1 where uid=#{uid} and order_time <= #{time} and cash=0")
    void updateCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select( "select IFNULL(sum(estimate_cos_price),0)  from mw_order_jd " +
            "where is_del=0")
    Double sumTotalPrice();

    @Select( "select IFNULL(sum(estimate_fee),0)  from mw_order_jd " +
            "where is_del=0")
    Double sumTotalFee();

    @Select("SELECT IFNULL(sum(estimate_cos_price),0) " +
            " FROM mw_order_jd ${ew.customSqlSegment}")
    Double sumPrice(@Param(Constants.WRAPPER) Wrapper<JunchenJdOrder> wrapper);

    @Select("SELECT IFNULL(sum(estimate_fee),0) " +
            " FROM mw_order_jd ${ew.customSqlSegment}")
    Double sumFee(@Param(Constants.WRAPPER) Wrapper<JunchenJdOrder> wrapper);

    @Select("SELECT IFNULL(sum(estimate_cos_price),0) as num," +
            "DATE_FORMAT(order_time, '%m-%d') as time " +
            " FROM mw_order_jd where is_del=0 and inner_type=0 and order_time >= #{time}" +
            " GROUP BY DATE_FORMAT(order_time,'%Y-%m-%d'), DATE_FORMAT(order_time, '%m-%d') " +
            " ORDER BY DATE_FORMAT(order_time, '%Y-%m-%d') ASC")
    List<ChartDataDto> chartList(@Param("time") Date time);
    @Select("SELECT count(id) as num," +
            "DATE_FORMAT(order_time, '%m-%d') as time " +
            " FROM mw_order_jd where is_del=0 and inner_type=0 and order_time >= #{time}" +
            " GROUP BY DATE_FORMAT(order_time,'%Y-%m-%d'), DATE_FORMAT(order_time, '%m-%d') " +
            " ORDER BY DATE_FORMAT(order_time, '%Y-%m-%d') ASC")
    List<ChartDataDto> chartListT(@Param("time") Date time);
    @Select("SELECT IFNULL(sum(estimate_fee),0) as num," +
            "DATE_FORMAT(order_time, '%m-%d') as time " +
            " FROM mw_order_jd where is_del=0 and inner_type=0 and order_time >= #{time}" +
            " GROUP BY DATE_FORMAT(order_time,'%Y-%m-%d'), DATE_FORMAT(order_time, '%m-%d') " +
            " ORDER BY DATE_FORMAT(order_time, '%Y-%m-%d') ASC")
    List<ChartDataDto> chartListFee(@Param("time") Date time);

//    @Update("update mw_order_jd set valid_code = 3" +
//            " where order_id in" +
//            " <foreach item='id' index='index' collection='ids' " +
//            " open='(' separator=',' close=')'>" +
//            " #{id}" +
//            " </foreach>")
//    void invalidRefundOrders(@Param("ids") List<Long> ids);
    @Update("update mw_order_jd set valid_code = 3,bind=3,hb=0.0" +
            " where order_id = #{id}")
    void invalidRefundOrders(@Param("id") Long id);

    @Select("SELECT IFNULL(sum(hb),0) " +
            " FROM mw_order_jd ${ew.customSqlSegment}")
    Double sumHb(@Param(Constants.WRAPPER) Wrapper<JunchenJdOrder> wrapper);

    @Select("SELECT uid,count(*) as orderCount " +
            " FROM mw_order_jd ${ew.customSqlSegment}")
    List<OrderCheckDTO> checkCount(@Param(Constants.WRAPPER) Wrapper<JunchenJdOrder> wrapper);
}
