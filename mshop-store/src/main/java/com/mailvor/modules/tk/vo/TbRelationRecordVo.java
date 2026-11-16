package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.mailvor.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


import java.io.Serializable;

/**
 * 大淘客「获取会员备案授权链接」响应（{@code get-relation-record}）。
 *
 * <p>推客拿到这条链接后去淘宝完成渠道备案，备案成功后其推广订单才能归因到本平台。</p>
 *
 * <p>上游不同版本的文档对承载链接的字段名有出入（{@code tb_relation_record} /
 * {@code relation_record} / {@code url}），这里全部兼容，解析时按优先级取第一个非空的。</p>
 */
@lombok.Data
@ApiModel(value = "TbRelationRecordVo", description = "会员备案授权链接响应")
public class TbRelationRecordVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 上游 code，0 表示成功 */
    @ApiModelProperty(value = "上游返回码，0成功")
    private Integer code;

    @ApiModelProperty(value = "上游返回描述")
    private String msg;

    private Data data;

    /** 上游是否返回成功 */
    @JSONField(serialize = false, deserialize = false)
    public boolean isSuccess() {
        return code != null && code == 0;
    }

    /**
     * 备案授权链接；上游未返回或返回空白时给出 null。
     *
     * <p>别名优先级：{@code tb_relation_record} → {@code relation_record} → {@code url}。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public String getRelationUrl() {
        if (!isSuccess() || data == null) {
            return null;
        }
        String[] candidates = {data.getTbRelationRecord(), data.getRelationRecord(), data.getUrl()};
        for (String candidate : candidates) {
            if (StringUtils.isNotBlank(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    @lombok.Data
    @ApiModel(value = "TbRelationRecordVo.Data", description = "备案授权链接数据")
    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 渠道备案授权链接（官方主字段名） */
        @JSONField(name = "tb_relation_record")
        @ApiModelProperty(value = "备案授权链接")
        private String tbRelationRecord;

        /** 兼容字段：部分文档版本写作 relation_record */
        @JSONField(name = "relation_record")
        @ApiModelProperty(value = "备案授权链接（兼容字段）")
        private String relationRecord;

        /** 兼容字段：部分文档版本写作 url */
        @JSONField(name = "url")
        @ApiModelProperty(value = "备案授权链接（兼容字段）")
        private String url;
    }
}
