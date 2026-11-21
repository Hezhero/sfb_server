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
import com.junchen.modules.tk.domain.JunchenTbOrder;
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
* @date 2022-08-29
*/
@Repository
public interface JunchenTbOrderMapper extends CoreMapper<JunchenTbOrder> {

    @Override
    @Select("SELECT * FROM mw_order_tb ${ew.customSqlSegment}")
    <E extends IPage<JunchenTbOrder>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<JunchenTbOrder> queryWrapper);

    @Update("update mw_order_tb set uid = ${uid} where trade_parent_id = #{id}")
    void bindUser(@Param("uid")Long uid, @Param("id") Long id);

    @Update("update mw_order_tb set bind = 2 where trade_parent_id = #{id}")
    void refundOrder(@Param("id") Long id);

    @Select("select COALESCE(sum(integral),0) from mw_order_tb " +
            "where uid=#{uid} and tk_create_time <= #{time} and bind=1 and cash=0")
    double totalCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select("select trade_parent_id from mw_order_tb where uid=#{uid} and tk_create_time <= #{time} and cash=0")
    Set<JunchenTbOrder> selectCashOrderId(@Param("uid") Long uid, @Param("time") Date time);

    @Update("update mw_order_tb set cash = 1 where uid=#{uid} and tk_create_time <= #{time} and cash=0")
    void updateCash(@Param("uid") Long uid, @Param("time") Date time);

    @Select( "select COALESCE(sum(alipay_total_price),0)  from mw_order_tb " +
            "where is_del=false")
    Double sumTotalPrice();

    @Select( "select COALESCE(sum(pub_share_pre_fee),0)  from mw_order_tb " +
            "where is_del=false")
    Double sumTotalFee();

    @Select("SELECT COALESCE(sum(alipay_total_price),0) " +
            " FROM mw_order_tb ${ew.customSqlSegment}")
    Double sumPrice(@Param(Constants.WRAPPER) Wrapper<JunchenTbOrder> wrapper);

    @Select("SELECT COALESCE(sum(pub_share_pre_fee),0) " +
            " FROM mw_order_tb ${ew.customSqlSegment}")
    Double sumFee(@Param(Constants.WRAPPER) Wrapper<JunchenTbOrder> wrapper);

    @Select("SELECT COALESCE(sum(alipay_total_price),0) as num," +
            "to_char(tk_create_time, 'MM-DD') as time " +
            " FROM mw_order_tb where is_del=false and inner_type=0 and tk_create_time >= #{time}" +
            " GROUP BY to_char(tk_create_time,'YYYY-MM-DD'), to_char(tk_create_time, 'MM-DD') " +
            " ORDER BY to_char(tk_create_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartList(@Param("time") Date time);
    @Select("SELECT count(trade_parent_id) as num," +
            "to_char(tk_create_time, 'MM-DD') as time " +
            " FROM mw_order_tb where is_del=false and inner_type=0 and tk_create_time >= #{time}" +
            " GROUP BY to_char(tk_create_time,'YYYY-MM-DD'), to_char(tk_create_time, 'MM-DD') " +
            " ORDER BY to_char(tk_create_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartListT(@Param("time") Date time);
    @Select("SELECT COALESCE(sum(pub_share_pre_fee),0) as num," +
            "to_char(tk_create_time, 'MM-DD') as time " +
            " FROM mw_order_tb where is_del=false and inner_type=0 and tk_create_time >= #{time}" +
            " GROUP BY to_char(tk_create_time,'YYYY-MM-DD'), to_char(tk_create_time, 'MM-DD') " +
            " ORDER BY to_char(tk_create_time, 'YYYY-MM-DD') ASC")
    List<ChartDataDto> chartListFee(@Param("time") Date time);

//    @Update("update mw_order_tb set tk_status = 13" +
//            " where trade_parent_id in" +
//            " <foreach collection='ids' index='index' item='id' separator=',' open='(' close=')'>" +
//            " #{id}" +
//            " </foreach>")
//    void invalidRefundOrders(@Param("ids") List<Long> ids);
    @Update("update mw_order_tb set tk_status = 13,bind=3,hb=0.0" +
        " where trade_parent_id = ${id}")
    void invalidRefundOrders(@Param("id") Long id);


    @Select("SELECT COALESCE(sum(hb),0) " +
            " FROM mw_order_tb ${ew.customSqlSegment}")
    Double sumHb(@Param(Constants.WRAPPER) Wrapper<JunchenTbOrder> wrapper);


    @Select("SELECT uid,count(*) as orderCount" +
            " FROM mw_order_tb ${ew.customSqlSegment}")
    List<OrderCheckDTO> checkCount(@Param(Constants.WRAPPER) Wrapper<JunchenTbOrder> wrapper);

}
