package com.junchen.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * 好单库「淘宝更高佣商品查询」响应（tblive_goods_query）。
 *
 * <p>官方返回核心字段：aduser_id / item_title / auction_price / itemsale /
 * object_pic / commission_rates / commission。</p>
 */
@Data
public class TbLiveGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private Data1 data;

    @lombok.Data
    public static class Data1 implements Serializable {
        @JSONField(name = "aduser_id")
        private String aduserId;
        @JSONField(name = "item_title")
        private String itemTitle;
        @JSONField(name = "auction_price")
        private Double auctionPrice;
        private Long itemsale;
        @JSONField(name = "object_pic")
        private String objectPic;
        @JSONField(name = "commission_rates")
        private Double commissionRates;
        /** 用于高佣转链的 promotion_id（官方规格：get_tblive_link 入参） */
        @JSONField(name = "promotion_id")
        private String promotionId;
    }

    /** 是否成功：官方成功码 1 或 200 */
    public boolean isOk() {
        return code != null && (code == 1 || code == 200);
    }
}
