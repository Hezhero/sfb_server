package com.mailvor.modules.tk.vo;

import lombok.Data;

/** 快手转链响应（ks_item_ratesurl）。 */
@Data
public class KsItemRatesUrlVo {
    private Integer code;
    private String msg;
    private KsItemRatesUrlData data;

    @Data
    public static class KsItemRatesUrlData {
        /** 快手口令 */
        private String linkCode;
        /** 快手落地页 */
        private String kwaiUrl;
        /** 短链 */
        private String linkUrl;
    }
}
