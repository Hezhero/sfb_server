package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 好单库闲鱼话费充值列表响应（xy_prepaid_recharge）。 */
@Data
public class IdleRechargeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    @JSONField(name = "min_id")
    private Integer minId;
    private List<Item> data;

    @lombok.Data
    public static class Item implements Serializable {
        private String itemid;
        @JSONField(name = "itempic")
        private String itemPic;
        @JSONField(name = "itemtitle")
        private String itemTitle;
        @JSONField(name = "itemprice")
        private Double itemPrice;
        @JSONField(name = "itemendprice")
        private Double itemEndPrice;
        @JSONField(name = "tkrates")
        private Double tkRates;
        @JSONField(name = "tkmoney")
        private Double tkMoney;
    }

    public boolean isOk() {
        return code != null && (code == 1 || code == 200);
    }
}
