package com.mailvor.modules.tk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mailvor.domain.BaseDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 大淘客商品库（工单 05）。
 *
 * <p><b>为什么新建这张表</b>：本仓库原先<b>没有</b>任何 CPS 商品库表——{@code mw_store_product}
 * 是自营商城商品（{@code price}/{@code stock}/{@code is_show} 等），字段与淘宝客商品
 * 完全不搭；淘宝客商品此前是每屏实时调大淘客接口，因此无法表达「已下架」
 * 「价格已变」这两种状态。工单 05 要解决的正是「C 端展示已下架/已变价商品」，
 * 前提是先有本地落库。</p>
 *
 * <p><b>匹配键是 {@code dtkId}（大淘客商品 id）</b>：官方失效商品接口
 * （api-d.html?id=11）在响应字段 {@code id} 上明确标注「大淘客商品id（用该ID进行匹配）」。
 * 所以本地必须用 {@code dtk_id} 作为业务唯一键，而不是 {@code goodsId}（淘宝商品 id）
 * ——后者在响应里是 String 且非必填，用它匹配会漏。</p>
 *
 * <p><b>为什么不用 {@code goods_sign} 匹配</b>：官方说明该字段「每次转链后都会变化」，
 * 且文档多处标注「暂时为空」，天然不适合当匹配键。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mw_dtk_goods")
@ApiModel(value = "MwDtkGoods对象", description = "大淘客商品库")
public class MwDtkGoods extends BaseDomain {

    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 大淘客商品 id —— 业务唯一键，也是失效检测的匹配键。
     *
     * <p>对应官方响应字段 {@code id}（Number）。不是 {@code goodsId}（淘宝商品 id），
     * 也不是 {@code goodsSign}（每次转链变化）。</p>
     */
    @ApiModelProperty(value = "大淘客商品id（业务唯一键，失效检测按此匹配）")
    private Long dtkId;

    @ApiModelProperty(value = "淘宝商品id")
    private String goodsId;

    @ApiModelProperty(value = "新商品id（每次转链后变化）")
    private String goodsSign;

    @ApiModelProperty(value = "商品淘宝链接")
    private String itemLink;

    @ApiModelProperty(value = "淘宝标题")
    private String title;

    @ApiModelProperty(value = "短标题")
    private String dtitle;

    @ApiModelProperty(value = "推广文案")
    private String description;

    @ApiModelProperty(value = "大淘客一级分类id")
    private Long cid;

    @ApiModelProperty(value = "大淘客二级分类id，多个用英文逗号分隔")
    private String subcid;

    @ApiModelProperty(value = "商品主图链接")
    private String mainPic;

    @ApiModelProperty(value = "营销主图链接")
    private String marketingMainPic;

    @ApiModelProperty(value = "商品原价")
    private Double originalPrice;

    @ApiModelProperty(value = "券后价")
    private Double actualPrice;

    @ApiModelProperty(value = "优惠券金额")
    private Double couponPrice;

    @ApiModelProperty(value = "优惠券链接")
    private String couponLink;

    @ApiModelProperty(value = "优惠券剩余量")
    private Long couponRemainCount;

    @ApiModelProperty(value = "优惠券领券量")
    private Long couponReceiveNum;

    @ApiModelProperty(value = "佣金比率")
    private Double commissionRate;

    @ApiModelProperty(value = "30天热销")
    private Long monthSales;

    @ApiModelProperty(value = "店铺名称")
    private String shopName;

    @ApiModelProperty(value = "淘宝卖家id")
    private String sellerId;

    @ApiModelProperty(value = "商品上架时间（官方 createTime）")
    private Date goodsCreateTime;

    // ----- 同步状态 -----

    /**
     * 商品状态：0-失效（已下架）1-正常。
     *
     * <p>默认 1（正常）：新入库的商品来自「商品更新/定时拉取」两个接口，
     * 上游只返回在架商品，所以入库即视为正常。失效只能由
     * {@code get-stale-goods-by-time} 的结果来标记。</p>
     */
    @ApiModelProperty(value = "商品状态：0失效 1正常")
    private Integer goodsStatus;

    @ApiModelProperty(value = "首次入库时间（来自哪个接口）")
    private String source;

    @ApiModelProperty(value = "失效时间（上游下架时间未知，取被检测到的时刻）")
    private Date staleTime;

    @ApiModelProperty(value = "最后一次同步时间")
    private Date lastSyncTime;

    /** 商品状态：正常 */
    public static final int STATUS_NORMAL = 1;

    /** 商品状态：失效 */
    public static final int STATUS_STALE = 0;

    /** 来源：商品更新 get-newest-goods */
    public static final String SOURCE_NEWEST = "newest";

    /** 来源：定时拉取 pull-goods-by-time */
    public static final String SOURCE_PULL = "pull";

    public boolean isStale() {
        return goodsStatus != null && goodsStatus == STATUS_STALE;
    }
}
