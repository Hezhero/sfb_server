package com.mailvor.modules.tk.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品更新 {@code get-newest-goods} 的单个商品（工单 05）。
 *
 * <p>字段逐项对齐官方响应参数表（api-d.html?id=3，parent_tree0）。官方该接口只返回
 * 价格类字段与分类字段，<b>不返回</b> title / mainPic / shopName —— 那些在
 * {@code pull-goods-by-time} 里才有。所以这个 VO 只声明官方真有的字段，
 * 不要为了「统一」把 {@link PullGoodsVo} 的字段抄过来，那会让调用方误以为有值。</p>
 */
@Data
@ApiModel(value = "NewestGoodsVo对象", description = "商品更新接口返回的商品")
public class NewestGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "大淘客商品id")
    private Long id;

    @ApiModelProperty(value = "淘宝商品id")
    private String goodsId;

    @ApiModelProperty(value = "新商品id（每次转链后变化，可能为空）")
    private String goodsSign;

    @ApiModelProperty(value = "原价")
    private Double originalPrice;

    @ApiModelProperty(value = "实际价格")
    private Double actualPrice;

    @ApiModelProperty(value = "券金额")
    private Double couponPrice;

    @ApiModelProperty(value = "折扣力度")
    private Double discounts;

    @ApiModelProperty(value = "佣金类型：0通用 1定向 2高佣 3营销计划")
    private Integer commissionType;

    @ApiModelProperty(value = "佣金比率")
    private Double commissionRate;

    @ApiModelProperty(value = "30天热销")
    private Long monthSales;

    @ApiModelProperty(value = "热推值")
    private Long hotPush;

    @ApiModelProperty(value = "商品在大淘客的二级分类id（可能多个）")
    private List<Long> subcid;

    @ApiModelProperty(value = "两小时热销")
    private Long twoHoursSales;

    @ApiModelProperty(value = "今日热销")
    private Long dailySales;

    @ApiModelProperty(value = "特色文案（官方示例里是数组形式的字符串）")
    private String specialText;

    @ApiModelProperty(value = "优惠券剩余量")
    private Long couponRemainCount;

    @ApiModelProperty(value = "优惠券领券量")
    private Long couponReceiveNum;

    @ApiModelProperty(value = "优惠券链接")
    private String couponLink;

    @ApiModelProperty(value = "优惠券id")
    private String couponId;

    @ApiModelProperty(value = "商品是否已经验货：0否 1是")
    private Integer inspectedGoods;
}
