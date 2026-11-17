package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * 好单库「淘宝更高佣商品转链」响应（get_tblive_link）。
 * 官方返回核心字段：deep_link。
 */
@Data
public class TbLiveLinkVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private Data1 data;

    @lombok.Data
    public static class Data1 implements Serializable {
        @JSONField(name = "deep_link")
        private String deepLink;
    }

    public boolean isOk() {
        return code != null && (code == 1 || code == 200);
    }
}
