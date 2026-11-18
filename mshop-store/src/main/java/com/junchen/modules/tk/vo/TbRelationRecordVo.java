package com.junchen.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.junchen.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


import java.io.Serializable;

/**
 * 大淘客「获取会员备案授权链接」响应（{@code get-relation-record}）。
 *
 * <p>官方文档 https://www.dataoke.com/api-d.html?id=135 的响应体只有一个字段：</p>
 *
 * <pre>
 * { "code": 0, "msg": "成功", "data": { "url": "备案授权链接" } }
 * </pre>
 *
 * <p>推客打开 {@code data.url} 去淘宝完成渠道备案，备案成功后其推广订单才能归因到本平台。</p>
 *
 * <p>历史实现曾对 {@code tb_relation_record} / {@code relation_record} 做过别名兜底，
 * 那是按推测写的；官方文档只定义了 {@code url}，故此处按官方规格收敛为单一字段。
 * 字段名解析仍走 fastjson 的 {@code @JSONField(name = "url")}，与文档一一对应。</p>
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
     * <p>字段名按官方文档取 {@code data.url}，不再做别名兜底。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public String getRelationUrl() {
        if (!isSuccess() || data == null) {
            return null;
        }
        return StringUtils.isBlank(data.getUrl()) ? null : data.getUrl().trim();
    }

    @lombok.Data
    @ApiModel(value = "TbRelationRecordVo.Data", description = "备案授权链接数据")
    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 渠道备案授权链接（官方唯一字段） */
        @JSONField(name = "url")
        @ApiModelProperty(value = "备案授权链接")
        private String url;
    }
}
