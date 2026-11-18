package com.junchen.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.junchen.modules.tk.domain.JunchenTbLiveOrder;
import lombok.Data;

import java.util.ArrayList;

/** 好单库淘宝更高佣订单响应（tb_live_order_list）。 */
@Data
public class TbLiveOrderResVo {
    private Integer code;
    private String msg;
    /** 下一页游标，空/0 终止 */
    @JSONField(name = "min_id")
    private Integer minId;
    private ArrayList<JunchenTbLiveOrder> data;
}
