package com.mailvor.modules.tk.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 大淘客 scroll_id 分页类接口的 data 包装（工单 05）。
 *
 * <p>三个接口（{@code get-newest-goods} / {@code get-stale-goods-by-time} /
 * {@code pull-goods-by-time}）的响应结构完全一致，都是：</p>
 * <pre>
 * {"time":1554711897417,"code":0,"msg":"成功",
 *  "data":{"list":[...],"totalNum":16499,"pageId":"76679471048598b0"}}
 * </pre>
 * 官方示例见 api-d.html?id=3 / id=11 / id=12 的「返回示例」。
 *
 * <p><b>{@code pageId} 是下一页游标</b>，不是页码。它由上游签发，本地必须原样回传，
 * 用于 {@code DtkScrollPager} 的翻页。</p>
 */
@Data
@ApiModel(value = "DtkScrollPageDataVo对象", description = "大淘客 scroll_id 分页数据")
public class DtkScrollPageDataVo<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "商品列表")
    private List<T> list;

    @ApiModelProperty(value = "符合条件的总数（用于监控任务进度，不用于本地分页）")
    private Long totalNum;

    @ApiModelProperty(value = "下一页游标（scroll_id），为空表示没有下一页")
    private String pageId;
}
