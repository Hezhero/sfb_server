package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/** 快手口令解析响应（ks_analyze_code）。 */
@Data
public class KsAnalyzeCodeVo {
    private Integer code;
    private String msg;
    private KsAnalyzeCodeData data;

    @Data
    public static class KsAnalyzeCodeData {
        @JSONField(name = "product_id")
        private String productId;
    }
}
