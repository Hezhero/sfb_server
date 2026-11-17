package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/** 快手商品列表/搜索响应（ks_item_list）。 */
@Data
public class KsGoodsListVo {
    private Integer code;
    private String msg;
    @JSONField(name = "min_id")
    private Integer minId;
    private List<KsGoodsVo> data;
}
