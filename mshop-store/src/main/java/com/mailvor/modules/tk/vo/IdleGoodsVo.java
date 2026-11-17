package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 好单库闲鱼商品列表/详情响应（idle_goods_query / idle_goods_detail）。
 * 两者返回字段相同。普通接口，apikey 鉴权。
 */
@Data
public class IdleGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    @JSONField(name = "min_id")
    private Integer minId;
    private List<Item> data;

    @lombok.Data
    public static class Item implements Serializable {
        @JSONField(name = "item_id")
        private String itemId;
        @JSONField(name = "item_title")
        private String itemTitle;
        @JSONField(name = "original_price")
        private Double originalPrice;
        @JSONField(name = "image_urls")
        private List<String> imageUrls;
        @JSONField(name = "reserve_price")
        private Double reservePrice;
        /** promoting推广中 / cannotPromote停止推广 / sold售出 / offline下架 / unknown未知 */
        private String status;
        @JSONField(name = "commission_rate")
        private Double commissionRate;
        @JSONField(name = "estimated_commission")
        private Double estimatedCommission;
    }

    public boolean isOk() {
        return code != null && (code == 1 || code == 200);
    }
}
