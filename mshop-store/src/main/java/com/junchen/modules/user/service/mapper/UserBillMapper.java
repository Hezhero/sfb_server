/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.user.domain.MwUserBill;
import com.junchen.modules.user.service.dto.BillOrderRecordDto;
import com.junchen.modules.user.service.dto.MUserBillDto;
import com.junchen.modules.user.service.dto.MwUserBillDto;
import com.junchen.modules.user.vo.BillVo;
import com.junchen.modules.user.vo.SignVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* @author mazhongjun
* @date 2020-05-12
*/
@Repository
public interface UserBillMapper extends CoreMapper<MwUserBill> {

    @Select("select COALESCE(sum(number),0) from mw_user_bill " +
            "where status=1 and biz_type='sign' and pm=1 and category='integral' " +
            "and uid=#{uid}")
    double sumIntegral(@Param("uid") Long uid);

    @Select("SELECT to_char(a.create_time,'YYYY-MM-DD') as addTime,a.title,a.number " +
            "FROM mw_user_bill a INNER JOIN mw_user u ON u.uid=a.uid WHERE a.category = 'integral'" +
            " AND a.biz_type = 'sign' AND a.status = 1 AND a.uid = #{uid} " +
            "ORDER BY to_char(a.create_time, 'YYYY-MM-DD') DESC")
    List<SignVo> getSignList(@Param("uid") Long uid, Page page);

    @Select("SELECT o.order_id as orderId,to_char(b.create_time, 'YYYY-MM-DD HH24:MI') as time," +
            "b.number,u.avatar,u.nickname FROM mw_user_bill b " +
            "INNER JOIN mw_store_order o ON o.id=b.link_id " +
            "RIGHT JOIN mw_user u ON u.uid=o.uid" +
            " WHERE b.uid = #{uid} AND ( to_char(b.create_time, 'YYYY-MM')= #{time} ) AND " +
            "b.category = 'now_money' AND b.biz_type = 'brokerage' ORDER BY time DESC")
    List<BillOrderRecordDto> getBillOrderRList(@Param("time") String time, @Param("uid") Long uid);

    @Select("SELECT to_char(create_time,'YYYY-MM') as time " +
            " FROM mw_user_bill ${ew.customSqlSegment}")
    List<String> getBillOrderList(@Param(Constants.WRAPPER) Wrapper<MwUserBill> userWrapper, Page page);

    @Select("SELECT to_char(create_time,'YYYY-MM') as time,string_agg(id, ',') ids " +
            " FROM mw_user_bill ${ew.customSqlSegment}")
    List<BillVo> getBillList(@Param(Constants.WRAPPER) Wrapper<MwUserBill> userWrapper, Page page);

    @Select("SELECT to_char(create_time,'YYYY-MM-DD HH24:MI') as add_time,to_char(create_time,'YYYY-MM-DD') as add_day,title,number,pm " +
            " FROM mw_user_bill ${ew.customSqlSegment}")
    List<MUserBillDto> getUserBillList(@Param(Constants.WRAPPER) Wrapper<MwUserBill> userWrapper);

    @Select("select COALESCE(sum(number),0) from mw_user_bill " +
            "where status=1 and biz_type='brokerage' and pm=1 and category='now_money' " +
            "and uid=#{uid}")
    double sumPrice(@Param("uid") int uid);

    @Select("select COALESCE(sum(number),0) from mw_user_bill " +
            "where status=1 and biz_type='recharge' and pm=1 and category='now_money' " +
            "and uid=#{uid}")
    double sumRechargePrice(@Param("uid") Long uid);


    @Select("select COALESCE(sum(number),0) from mw_user_bill " +
            "where status=1 and biz_type='brokerage' and pm=1 and category='now_money' " +
            "and uid=#{uid} and create_time >= CURRENT_TIMESTAMP - INTERVAL '1 day'")
    double sumYesterdayPrice(@Param("uid") Long uid);

    @Select("<script> select b.title,b.pm,b.mark,b.uid,b.category,b.biz_type as type,b.number,b.create_time ,u.nickname " +
            "from mw_user_bill b left join mw_user u on u.uid=b.uid  where 1=1  "  +
            "<if test =\"category !=''\">and b.category=#{category}</if> " +
            "<if test =\"type !=''\">and b.biz_type=#{type}</if> " +
            "<if test =\"title !=''\">and b.title=#{title}</if> " +
            "<if test =\"pm !=null\">and b.pm=#{pm}</if> " +
            "<if test =\"date !=null\">and b.create_time &gt;= to_timestamp(#{date},'YYYY-MM-DD HH24:MI:SS')</if> " +
            "<if test =\"date1 !=null\">and b.create_time &lt;=to_timestamp(#{date1},'YYYY-MM-DD HH24:MI:SS')</if> " +
            "<if test =\"search !=''\">and (b.uid LIKE CONCAT('%',#{search},'%') or b.mark LIKE CONCAT('%',#{search},'%') or u.nickname LIKE CONCAT('%',#{search},'%'))</if>" +
            "order by b.create_time desc </script> ")
    List<MwUserBillDto> findAllByQueryCriteria(@Param("category") String category, @Param("type") String type, @Param("search") String search, @Param("pm") Integer pm, @Param("date")String date, @Param("date1")String date1, @Param("title")String title);

    @Update( "update mw_user_bill set unlock_status = 2 where link_id = #{id}")
    void invalidByOrderId(@Param("id") String id);

    @Select("select COALESCE(sum(number),0) from mw_user_bill " +
            "where unlock_status=1 and category='now_money' " +
            "and uid=#{uid}")
    double sumUnlockMoney(@Param("uid") Long uid);

}
