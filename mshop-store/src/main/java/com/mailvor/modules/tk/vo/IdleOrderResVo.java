package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.mailvor.modules.tk.domain.MailvorIdleOrder;
import lombok.Data;

import java.util.ArrayList;

/** 好单库闲鱼订单响应（idle_order_list）。 */
@Data
public class IdleOrderResVo {
    private Integer code;
    private String msg;
    @JSONField(name = "min_id")
    private Integer minId;
    private ArrayList<MailvorIdleOrder> data;
}
