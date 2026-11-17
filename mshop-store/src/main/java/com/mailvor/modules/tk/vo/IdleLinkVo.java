package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/** 好单库闲鱼推广转链响应（idle_general_link）。 */
@Data
public class IdleLinkVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private D data;

    @lombok.Data
    public static class D implements Serializable {
        @JSONField(name = "short_url")
        private String shortUrl;
        @JSONField(name = "short_tpwd")
        private String shortTpwd;
        private String deeplink;
    }

    public boolean isOk() {
        return code != null && (code == 1 || code == 200);
    }
}
