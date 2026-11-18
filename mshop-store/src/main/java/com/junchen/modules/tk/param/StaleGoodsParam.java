package com.junchen.modules.tk.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.TreeMap;

/**
 * 失效商品 {@code get-stale-goods-by-time} 请求参数（工单 05）。
 *
 * <p>官方文档：{@code https://www.dataoke.com/pmc/api-d.html?id=11}，当前版本 {@code v1.0.1}。</p>
 *
 * <p><b>时间窗口限制 24 小时</b>：官方对 {@code startTime} 的说明是
 * 「商品下架的时间大于等于开始时间（查询时间24小时以内）」。
 * 窗口收敛的职责在调用方（{@code DtkGoodsSyncService} 的窗口计算），
 * 本类是纯参数载体，不做时间运算——参数对象只负责「把值送上去」。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "StaleGoodsParam对象", description = "失效商品请求参数")
public class StaleGoodsParam extends DtkScrollPageParam {
    private static final long serialVersionUID = 1L;

    /** get-stale-goods-by-time 官方 pageSize 默认值 100 */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /** 官方仅允许查询最近 24 小时的下架记录 */
    public static final long MAX_WINDOW_MILLIS = 24L * 60 * 60 * 1000;

    @ApiModelProperty(value = "商品下架时间 >= startTime，查询范围限 24 小时以内，yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @ApiModelProperty(value = "商品下架时间 <= endTime，默认当前时间，yyyy-MM-dd HH:mm:ss")
    private String endTime;

    /** 默认每页 100 条（官方 get-stale-goods-by-time 的 pageSize 默认值） */
    public TreeMap<String, Object> toParamMap() {
        return toParamMap(DEFAULT_PAGE_SIZE);
    }
}
