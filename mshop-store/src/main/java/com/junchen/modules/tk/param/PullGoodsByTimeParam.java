package com.junchen.modules.tk.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.TreeMap;

/**
 * 定时拉取 {@code pull-goods-by-time} 请求参数（工单 05）。
 *
 * <p>官方文档：{@code https://www.dataoke.com/pmc/api-d.html?id=12}，当前版本 {@code v1.2.3}。</p>
 *
 * <p>注意两个与 {@code get-newest-goods} 不同的地方：</p>
 * <ul>
 *   <li>分类参数是单数 {@code cid}（不是 {@code cids}），且只有一个二级分类 {@code subcid}</li>
 *   <li>pageSize 官方默认 10，另有 {@code pre}（是否预告商品）与
 *       {@code freeshipRemoteDistrict}（偏远地区包邮）两个独有筛选</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "PullGoodsByTimeParam对象", description = "定时拉取商品请求参数")
public class PullGoodsByTimeParam extends DtkScrollPageParam {
    private static final long serialVersionUID = 1L;

    /** pull-goods-by-time 官方 pageSize 默认值 10 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    @ApiModelProperty(value = "大淘客一级分类id")
    private String cid;

    @ApiModelProperty(value = "大淘客二级分类id（与 cid 同时传时被上游忽略）")
    private Integer subcid;

    @ApiModelProperty(value = "是否预告商品：1预告商品 0所有商品")
    private Integer pre;

    @ApiModelProperty(value = "排序：0综合 1上架时间 2热销 3领券量 4佣金比例 5券后价高到低 6券后价低到高")
    private String sort;

    @ApiModelProperty(value = "商品上架时间 >= startTime，yyyy-mm-dd hh:mm:ss")
    private String startTime;

    @ApiModelProperty(value = "商品上架时间 <= endTime，默认当前时间，yyyy-mm-dd hh:mm:ss")
    private String endTime;

    @ApiModelProperty(value = "偏远地区包邮：1是 0否")
    private Integer freeshipRemoteDistrict;

    @ApiModelProperty(value = "是否精选商品：1精选")
    private Integer choice;

    @ApiModelProperty(value = "是否有券：0全部 1有券 -1无券，默认1")
    private Integer hasCoupon;

    /** 默认每页 10 条（官方 pull-goods-by-time 的 pageSize 默认值） */
    public TreeMap<String, Object> toParamMap() {
        return toParamMap(DEFAULT_PAGE_SIZE);
    }
}
