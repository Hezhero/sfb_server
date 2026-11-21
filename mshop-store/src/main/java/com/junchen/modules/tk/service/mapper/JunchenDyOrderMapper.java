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
import com.junchen.modules.tk.domain.JunchenDyOrder;
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
* @date 2022-09-07
*/
@Repository
public interface JunchenDyOrderMapper extends CoreMapper<JunchenDyOrder> {

    @Override
    @Select("SELECT * FROM mw_order_dy ${ew.customSqlSegment}")
    <E extends IPage<JunchenDyOrder>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<JunchenDyOrder> queryWrapper);

    @Update("update mw_order_dy set uid = ${uid} where order_id = #{id}")
    void bindUser(@Param("uid")Long uid, @Param("id") String orderId);
    @Update("update mw_order_dy set bind = 2 where order_id = #{id}")
    void refundOrder(@Param("id") String orderId);


    @Select("select COALESCE(sum(integral),0) from mw_order_dy " +
            "where uid=#{uid} and pay_success_time <= #{time} and bind=1 and cash=0")
    double totalCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select("select order_id from mw_order_dy where uid=#{uid} and pay_success_time <= #{time} and cash=0")
    Set<JunchenDyOrder> selectCashOrderId(@Param("uid") Long uid, @Param("time") Date time);


    @Update("update mw_order_dy set cash = 1 where uid=#{uid} and pay_success_time <= #{time} and cash=0")
    void updateCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select( "select COALESCE(sum(total_pay_amount),0)  from mw_order_dy " +
            "where is_del=false")
    Double sumTotalPrice();

    @Select( "select COALESCE(sum(estimated_total_commission),0)  from mw_order_dy " +
            "where is_del=false")
    Double sumTotalFee();

    @Select("SELECT COALESCE(sum(total_pay_amount),0) " +
            " FROM mw_order_dy ${ew.customSqlSegment}")
    Double sumPrice(@Param(Constants.WRAPPER) Wrapper<JunchenDyOrder> wrapper);

    @Select("SELECT COALESCE(sum(estimated_total_commission),0) " +
            " FROM mw_order_dy ${ew.customSqlSegment}")
    Double sumFee(@Param(Constants.WRAPPER) Wrapper<JunchenDyOrder> wrapper);

    @Select("SELECT COALESCE(sum(total_pay_amount),0) as num," +
            "to_char(pay_success_time, 'MM-DD') as time " +
            " FROM mw_order_dy where is_del=false and inner_type=0 and pay_success_time >= #{time}" +
            " GROUP BY to_char(pay_success_time,'YYYY-MM-DD'), to_char(pay_success_time, 'MM-DD') " +
            " ORDER BY to_char(pay_success_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartList(@Param("time") Date time);
    @Select("SELECT count(order_id) as num," +
            "to_char(pay_success_time, 'MM-DD') as time " +
            " FROM mw_order_dy where is_del=false and inner_type=0 and pay_success_time >= #{time}" +
            " GROUP BY to_char(pay_success_time,'YYYY-MM-DD'), to_char(pay_success_time, 'MM-DD') " +
            " ORDER BY to_char(pay_success_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartListT(@Param("time") Date time);
    @Select("SELECT COALESCE(sum(estimated_total_commission),0) as num," +
            "to_char(pay_success_time, 'MM-DD') as time " +
            " FROM mw_order_dy where is_del=false and inner_type=0 and pay_success_time >= #{time}" +
            " GROUP BY to_char(pay_success_time,'YYYY-MM-DD'), to_char(pay_success_time, 'MM-DD') " +
            " ORDER BY to_char(pay_success_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartListFee(@Param("time") Date time);

//    @Update("update mw_order_dy set flow_point = 'REFUND'" +
//            " where order_id in" +
//            " <foreach item='id' index='index' collection='ids' " +
//            " open='(' separator=',' close=')'>" +
//            " #{id}" +
//            " </foreach>")
//    void invalidRefundOrders(@Param("ids") List<String> ids);
    @Update("update mw_order_dy set flow_point = 'REFUND',bind=3,hb=0.0" +
            " where order_id = #{id}")
    void invalidRefundOrders(@Param("id") String id);

    @Select("SELECT COALESCE(sum(hb),0) " +
            " FROM mw_order_dy ${ew.customSqlSegment}")
    Double sumHb(@Param(Constants.WRAPPER) Wrapper<JunchenDyOrder> wrapper);

    @Select("SELECT uid,count(*) as orderCount " +
            " FROM mw_order_dy ${ew.customSqlSegment}")
    List<OrderCheckDTO> checkCount(@Param(Constants.WRAPPER) Wrapper<JunchenDyOrder> wrapper);

}
