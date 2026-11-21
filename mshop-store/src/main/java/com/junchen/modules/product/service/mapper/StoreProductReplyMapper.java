/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.product.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.product.domain.MwStoreProductReply;
import com.junchen.modules.product.vo.MwStoreProductReplyQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
* @author huangyu
* @date 2020-05-12
*/
@Repository
public interface StoreProductReplyMapper extends CoreMapper<MwStoreProductReply> {

    @Select("select A.product_score as productScore,A.service_score as serviceScore," +
            "A.biz_comment as comment,A.merchant_reply_content as merchantReplyContent," +
            "A.merchant_reply_time as merchantReplyTime,A.pics as pictures,A.create_Time as createTime," +
            "B.nickname,B.avatar,C.cart_info as cartInfo" +
            " from mw_store_product_reply A left join mw_user B " +
            "on A.uid = B.uid left join mw_store_order_cart_info C on A.biz_unique = C.biz_unique" +
            " where A.product_id=#{productId} and A.is_del=false " +
            "order by A.create_Time DESC limit 1")
    MwStoreProductReplyQueryVo getReply(long productId);

    @Select("<script>select A.product_score as productScore,A.service_score as serviceScore," +
            "A.biz_comment as comment,A.merchant_reply_content as merchantReplyContent," +
            "A.merchant_reply_time as merchantReplyTime,A.pics as pictures,A.create_Time as createTime," +
            "B.nickname,B.avatar,C.cart_info as cartInfo" +
            " from mw_store_product_reply A left join mw_user B " +
            "on A.uid = B.uid left join mw_store_order_cart_info C on A.biz_unique = C.biz_unique" +
            " where A.product_id=#{productId} and A.is_del=false " +
            "<if test='type == 1'>and A.product_score = 5</if>" +
            "<if test='type == 2'>and A.product_score &lt; 5 and A.product_score &gt; 2</if>" +
            "<if test='type == 3'>and A.product_score &lt; 2</if>"+
            " order by A.create_Time DESC</script>")
    List<MwStoreProductReplyQueryVo> selectReplyList(Page page, @Param("productId") long productId, @Param("type") int type);

}
