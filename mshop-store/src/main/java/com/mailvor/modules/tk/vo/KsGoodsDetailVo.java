package com.mailvor.modules.tk.vo;

import lombok.Data;

/** 快手商品详情响应（ks_item_detail）。 */
@Data
public class KsGoodsDetailVo {
    private Integer code;
    private String msg;
    private KsGoodsVo data;
}
