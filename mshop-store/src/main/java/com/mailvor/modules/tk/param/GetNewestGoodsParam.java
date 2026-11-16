package com.mailvor.modules.tk.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.TreeMap;

/**
 * 商品更新 {@code get-newest-goods} 请求参数（工单 05）。
 *
 * <p>官方文档：{@code https://www.dataoke.com/pmc/api-d.html?id=3}，当前版本 {@code v1.2.0}。
 * 字段名即上游参数名（{@link #toParamMap()} 靠 fastjson 直接映射），改名会直接导致参数失效。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "GetNewestGoodsParam对象", description = "商品更新请求参数")
public class GetNewestGoodsParam extends DtkScrollPageParam {
    private static final long serialVersionUID = 1L;

    /** get-newest-goods 官方 pageSize 默认值 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    @ApiModelProperty(value = "商品上架开始时间，yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @ApiModelProperty(value = "商品上架结束时间，yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @ApiModelProperty(value = "大淘客一级分类id，多个用英文逗号分隔")
    private String cids;

    @ApiModelProperty(value = "大淘客二级分类id（与 cids 同时传时被上游忽略）")
    private Integer subcid;

    @ApiModelProperty(value = "排序：0综合 1上架时间 2热销 3领券量 4佣金比例 5券后价高到低 6券后价低到高")
    private String sort;

    @ApiModelProperty(value = "是否有券：0全部 1有券 -1无券，默认1")
    private Integer hasCoupon;

    /** 默认每页 10 条（官方 get-newest-goods 的 pageSize 默认值） */
    public TreeMap<String, Object> toParamMap() {
        return toParamMap(DEFAULT_PAGE_SIZE);
    }
}
