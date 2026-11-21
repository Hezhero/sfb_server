/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.activity.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.activity.domain.MwStoreCouponIssue;
import com.junchen.modules.activity.vo.MwStoreCouponIssueQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* @author huangyu
* @date 2020-05-13
*/
@Repository
public interface MwStoreCouponIssueMapper extends CoreMapper<MwStoreCouponIssue> {
    @Select("<script>select A.cid,A.end_time as endTime,A.start_time as startTime,A.cname,A.ctype," +
            "A.is_permanent as isPermanent,A.remain_count as remainCount," +
            "A.total_count as totalCount,A.id,B.coupon_price as couponPrice," +
            "B.use_min_price as useMinPrice" +
            " from mw_store_coupon_issue A left join mw_store_coupon B " +
            "on A.cid = B.id " +
            "where A.status =1 <if test='type == 1'> AND B.biz_type = #{type} AND #{productId} = ANY(string_to_array(product_id, ','))</if> " +
            "<if test='type == 0'> AND B.biz_type in (0,1)</if>" +
            " AND (  A.start_time &lt; now()  AND A.end_time &gt; now() ) " +
            " AND A.is_del = false  AND " +
            "( A.remain_count > 0 OR A.is_permanent = true ) ORDER BY B.sort DESC</script>")
    List<MwStoreCouponIssueQueryVo> selecCoupontList(Page page, @Param("type") Integer type,
                                                     @Param("productId") Long productId);

    @Select("select A.cid,A.end_time as endTime,A.start_time as startTime," +
            "A.is_permanent as isPermanent,A.remain_count as remainCount," +
            "A.total_count as totalCount,A.id" +
            " from mw_store_coupon_issue A" +
            " where A.status =1 and A.id=#{id}" +
            " AND (  A.start_time < now()  AND A.end_time > now() ) " +
            " AND A.is_del = false  AND " +
            "( A.remain_count > 0 OR A.is_permanent = true )")
    MwStoreCouponIssueQueryVo selectOne(Integer id);

    @Update("update mw_store_coupon_issue set remain_count=remain_count-1" +
            " where id=#{id}")
    int decCount(@Param("id") int id);
}
