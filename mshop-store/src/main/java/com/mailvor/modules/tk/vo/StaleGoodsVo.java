package com.mailvor.modules.tk.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 失效商品 {@code get-stale-goods-by-time} 的单个商品（工单 05）。
 *
 * <p>官方响应只有三个字段（api-d.html?id=11，parent_tree0）：
 * {@code id} / {@code goodsId} / {@code goodsSign}。这是三个接口里最简的一个。</p>
 *
 * <p><b>用 {@code id} 匹配本地商品</b>：官方示例值直接标了
 * 「{@code id} — 大淘客商品id（用该ID进行匹配）」。因此本地商品库必须以
 * 大淘客商品 id 作为匹配键落库（见 {@code MwDtkGoods.dtkId}）。</p>
 */
@Data
@ApiModel(value = "StaleGoodsVo对象", description = "失效商品接口返回的商品")
public class StaleGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "大淘客商品id（用该ID匹配本地商品）")
    private Long id;

    @ApiModelProperty(value = "淘宝商品id")
    private String goodsId;

    @ApiModelProperty(value = "新商品id（每次转链后变化，可能为空）")
    private String goodsSign;
}
