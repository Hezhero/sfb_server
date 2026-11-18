package com.junchen.modules.tk.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 定时拉取 {@code pull-goods-by-time} 的单个商品（工单 05）。
 *
 * <p>字段逐项对齐官方响应参数表（api-d.html?id=12，parent_tree0，约 50 个字段）。
 * 这是三个接口里最全的一个：价格、标题、图片、店铺、券、活动、DSR 评分都有，
 * 也是本地商品库字段的唯一完整来源。</p>
 */
@Data
@ApiModel(value = "PullGoodsVo对象", description = "定时拉取接口返回的商品")
public class PullGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    // ----- 标识 -----

    @ApiModelProperty(value = "大淘客商品id")
    private Long id;

    @ApiModelProperty(value = "淘宝商品id")
    private String goodsId;

    @ApiModelProperty(value = "新商品id（每次转链后变化，可能为空）")
    private String goodsSign;

    @ApiModelProperty(value = "商品淘宝链接")
    private String itemLink;

    // ----- 文案 -----

    @ApiModelProperty(value = "淘宝标题")
    private String title;

    @ApiModelProperty(value = "短标题")
    private String dtitle;

    @ApiModelProperty(value = "推广文案")
    private String desc;

    // ----- 分类 -----

    @ApiModelProperty(value = "商品在大淘客的一级分类id")
    private Long cid;

    @ApiModelProperty(value = "商品在大淘客的二级分类id（可能多个）")
    private List<Long> subcid;

    @ApiModelProperty(value = "商品在淘宝的分类id")
    private Long tbcid;

    // ----- 图片/视频 -----

    @ApiModelProperty(value = "商品主图链接")
    private String mainPic;

    @ApiModelProperty(value = "营销主图链接")
    private String marketingMainPic;

    @ApiModelProperty(value = "商品视频")
    private String video;

    // ----- 价格 -----

    @ApiModelProperty(value = "商品原价")
    private Double originalPrice;

    @ApiModelProperty(value = "券后价")
    private Double actualPrice;

    @ApiModelProperty(value = "折扣力度")
    private Double discounts;

    // ----- 佣金 -----

    @ApiModelProperty(value = "佣金类型：0通用 1定向 2高佣 3营销计划")
    private Integer commissionType;

    @ApiModelProperty(value = "佣金比例")
    private Double commissionRate;

    // ----- 优惠券 -----

    @ApiModelProperty(value = "优惠券链接")
    private String couponLink;

    @ApiModelProperty(value = "券总量")
    private Long couponTotalNum;

    @ApiModelProperty(value = "领券量")
    private Long couponReceiveNum;

    @ApiModelProperty(value = "优惠券结束时间")
    private String couponEndTime;

    @ApiModelProperty(value = "优惠券开始时间")
    private String couponStartTime;

    @ApiModelProperty(value = "优惠券金额")
    private Double couponPrice;

    @ApiModelProperty(value = "优惠券使用条件")
    private String couponConditions;

    // ----- 销量 -----

    @ApiModelProperty(value = "30天热销")
    private Long monthSales;

    @ApiModelProperty(value = "2小时热销")
    private Long twoHoursSales;

    @ApiModelProperty(value = "当天热销")
    private Long dailySales;

    // ----- 品牌 -----

    @ApiModelProperty(value = "是否品牌商品：0否 1是")
    private Integer brand;

    @ApiModelProperty(value = "品牌id")
    private Long brandId;

    @ApiModelProperty(value = "品牌名称")
    private String brandName;

    // ----- 时间 -----

    @ApiModelProperty(value = "商品上架时间")
    private String createTime;

    @ApiModelProperty(value = "活动开始时间")
    private String activityStartTime;

    @ApiModelProperty(value = "活动结束时间")
    private String activityEndTime;

    @ApiModelProperty(value = "活动类型：1无活动 2淘抢购 3聚划算")
    private Integer activityType;

    // ----- 店铺 -----

    @ApiModelProperty(value = "店铺类型：1天猫 0淘宝")
    private Integer shopType;

    @ApiModelProperty(value = "淘宝卖家id")
    private String sellerId;

    @ApiModelProperty(value = "店铺名称")
    private String shopName;

    @ApiModelProperty(value = "淘宝店铺等级")
    private Integer shopLevel;

    @ApiModelProperty(value = "店铺logo")
    private String shopLogo;

    // ----- 评分 -----

    @ApiModelProperty(value = "描述分")
    private Double descScore;

    @ApiModelProperty(value = "描述相符")
    private Double dsrScore;

    @ApiModelProperty(value = "描述同行比")
    private Double dsrPercent;

    @ApiModelProperty(value = "服务态度（官方字段名 shipScore）")
    private Double shipScore;

    @ApiModelProperty(value = "服务同行比")
    private Double shipPercent;

    @ApiModelProperty(value = "物流服务（官方字段名 serviceScore）")
    private Double serviceScore;

    @ApiModelProperty(value = "物流同行比")
    private Double servicePercent;

    // ----- 其他 -----

    @ApiModelProperty(value = "热推值")
    private Long hotPush;

    @ApiModelProperty(value = "放单人名称")
    private String teamName;

    @ApiModelProperty(value = "定金，无定金为0")
    private Double quanMLink;

    @ApiModelProperty(value = "立减，无立减为0")
    private Double hzQuanOver;

    @ApiModelProperty(value = "是否包运费险：0不包 1包")
    private Integer yunfeixian;

    @ApiModelProperty(value = "预估淘礼金")
    private Double estimateAmount;

    @ApiModelProperty(value = "偏远地区包邮：0不包 1包邮")
    private Integer freeshipRemoteDistrict;

    @ApiModelProperty(value = "商品是否已经验货：0否 1是")
    private Integer inspectedGoods;
}
