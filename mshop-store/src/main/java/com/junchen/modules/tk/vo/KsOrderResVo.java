package com.junchen.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.junchen.modules.tk.domain.JunchenKsKuOrder;
import lombok.Data;

import java.util.ArrayList;

/** 快手订单拉取响应（ks_order_list）。 */
@Data
public class KsOrderResVo {
    private Integer code;
    private String msg;
    /** 下一页游标，传空/0 终止 */
    @JSONField(name = "min_id")
    private Integer minId;
    private ArrayList<JunchenKsKuOrder> data;
}
