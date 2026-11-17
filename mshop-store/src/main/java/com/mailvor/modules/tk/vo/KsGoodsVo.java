package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * 快手商品（好单库 ks_item_list / ks_item_detail 公共字段）。
 * 字段名严格按官方规格：itemtitle / itemprice / itemendprice / itempic / sales / ksmoney / ksrates。
 */
@Data
public class KsGoodsVo {
    @JSONField(name = "product_id")
    private String productId;
    private String itemtitle;
    private Double itemprice;
    private Double itemendprice;
    private String itempic;
    private Integer sales;
    /** 佣金金额 */
    private Double ksmoney;
    /** 佣金比率 */
    private Double ksrates;
    /** 详情接口独有：轮播图 */
    @JSONField(name = "ks_images")
    private List<String> ksImages;
    /** 详情接口独有：详情图 */
    @JSONField(name = "ks_detail_imgs")
    private List<String> ksDetailImgs;
    /** 详情接口独有：品牌名 */
    @JSONField(name = "brand_name")
    private String brandName;
}
