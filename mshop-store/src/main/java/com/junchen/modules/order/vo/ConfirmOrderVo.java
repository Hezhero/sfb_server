package com.junchen.modules.order.vo;

import com.junchen.modules.activity.vo.StoreCouponUserVo;
import com.junchen.modules.cart.vo.MwStoreCartQueryVo;
import com.junchen.modules.order.service.dto.PriceGroupDto;
import com.junchen.modules.product.vo.MwSystemStoreQueryVo;
import com.junchen.modules.user.domain.MwUserAddress;
import com.junchen.modules.user.vo.MwUserQueryVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName ConfirmOrderVo
 * @author huangyu
 * @Date 2019/10/27
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmOrderVo implements Serializable {
    //地址信息
    private MwUserAddress addressInfo;

    //砍价id
    private Integer bargainId;

    private List<MwStoreCartQueryVo> cartInfo;

    private Integer combinationId;

    //优惠券减
    private Boolean deduction;

    private Boolean enableIntegral;

    private Double enableIntegralNum;

    //积分抵扣
    private Integer integralRatio;

    private String orderKey;

    private PriceGroupDto priceGroup;

    private Integer seckillId;

    //店铺自提
    private Integer storeSelfMention;

    //店铺信息
    private MwSystemStoreQueryVo systemStore;


    private StoreCouponUserVo usableCoupon;

    private MwUserQueryVo userInfo;



}
