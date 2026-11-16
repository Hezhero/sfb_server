package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


import java.io.Serializable;

/**
 * 大淘客「查询会员备案信息」响应（{@code get-publisher-info}）。
 *
 * <p>用于判断某个推客（会员）是否已与平台建立淘宝渠道备案关系。
 * 未备案的推客推广产生的订单无法归因到本平台，佣金链从源头就断。</p>
 *
 * <p>字段名按官方文档使用下划线风格，Java 侧用驼峰承载。</p>
 */
@lombok.Data
@ApiModel(value = "TbPublisherInfoVo", description = "会员备案信息响应")
public class TbPublisherInfoVo implements Serializable {
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
     * 是否已完成备案。
     *
     * <p>只有上游明确返回 {@code relation_status=1} 才算已备案；字段缺失、
     * data 为空、上游报错一律视为未备案，避免把未知状态当成可以结算。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public boolean isFiled() {
        return isSuccess() && getRelationStatus() != null && getRelationStatus() == 1;
    }

    /** 备案状态：1-已备案 0-未备案。data 为空时返回 null。 */
    @JSONField(serialize = false, deserialize = false)
    public Integer getRelationStatus() {
        return data == null ? null : data.getRelationStatus();
    }

    /** 备案后淘宝分配的专用推广位。data 为空时返回 null。 */
    @JSONField(serialize = false, deserialize = false)
    public String getSpecialPid() {
        return data == null ? null : data.getSpecialPid();
    }

    @lombok.Data
    @ApiModel(value = "TbPublisherInfoVo.Data", description = "会员备案信息")
    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 备案状态：1-已备案 0-未备案 */
        @JSONField(name = "relation_status")
        @ApiModelProperty(value = "备案状态：1已备案 0未备案")
        private Integer relationStatus;

        /** 备案后淘宝分配的专用推广位 */
        @JSONField(name = "special_pid")
        @ApiModelProperty(value = "专用推广位")
        private String specialPid;
    }
}
