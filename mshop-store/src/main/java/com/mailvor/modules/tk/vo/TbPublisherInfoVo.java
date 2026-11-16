package com.mailvor.modules.tk.vo;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


import java.io.Serializable;
import java.util.List;

/**
 * 大淘客「查询会员备案信息」响应（{@code get-publisher-info}）。
 *
 * <p>官方文档 https://www.dataoke.com/api-d.html?id=136 定义的响应结构：</p>
 *
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "成功",
 *   "data": {
 *     "total_count": 1,              // 备案记录总数
 *     "inviter_list": [              // 备案列表
 *       { "account_name": "会员淘宝昵称",
 *         "create_date": "备案时间",
 *         "special_id": 12345,       // 会员关系 ID（sid）
 *         "root_pid": "mm_x_y_z" }   // 推广位 PID
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p>用于判断某个推客（会员）是否已与平台建立淘宝渠道备案关系。
 * 未备案的推客推广产生的订单无法归因到本平台，佣金链从源头就断。</p>
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
     * <p><b>注意：这是推断，不是官方语义。</b>官方文档没有给出任何状态字段，
     * 只返回 {@code total_count}（备案记录总数）与 {@code inviter_list}（备案列表）。
     * 因此这里按「{@code total_count > 0} 且 {@code inviter_list} 非空」判定为已备案：
     * 两个条件同时满足才认为上游确实返回了备案记录。</p>
     *
     * <p>保守方向：字段缺失、data 为空、上游报错一律视为未备案，
     * 避免把未知状态当成可以结算（错误偏向「多提示一次备案」而非「漏发佣金」）。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public boolean isFiled() {
        if (!isSuccess() || data == null) {
            return false;
        }
        Integer total = data.getTotalCount();
        return total != null && total > 0
                && data.getInviterList() != null && !data.getInviterList().isEmpty();
    }

    /** 备案记录总数；data 为空时返回 null。 */
    @JSONField(serialize = false, deserialize = false)
    public Integer getTotalCount() {
        return data == null ? null : data.getTotalCount();
    }

    /** 备案列表；data 为空时返回 null。 */
    @JSONField(serialize = false, deserialize = false)
    public List<Inviter> getInviterList() {
        return data == null ? null : data.getInviterList();
    }

    /**
     * 取最新一条备案记录（非空列表的第一条），列表为空时返回 null。
     *
     * <p>调用方用它落库 {@code special_id} / {@code root_pid} 等官方返回字段；
     * 多条记录时以第一条为准，同时由 {@code total_count} 记录总数。</p>
     */
    @JSONField(serialize = false, deserialize = false)
    public Inviter getLatestInviter() {
        List<Inviter> list = getInviterList();
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @lombok.Data
    @ApiModel(value = "TbPublisherInfoVo.Data", description = "会员备案信息")
    public static class Data implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 备案记录总数 */
        @JSONField(name = "total_count")
        @ApiModelProperty(value = "备案记录总数")
        private Integer totalCount;

        /** 备案列表 */
        @JSONField(name = "inviter_list")
        @ApiModelProperty(value = "备案列表")
        private List<Inviter> inviterList;
    }

    /**
     * 单条备案记录（官方 inviter_list 的元素）。
     *
     * <p>字段按官方文档使用下划线风格，Java 侧用驼峰承载。</p>
     */
    @lombok.Data
    @ApiModel(value = "TbPublisherInfoVo.Inviter", description = "单条会员备案记录")
    public static class Inviter implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 会员淘宝昵称 */
        @JSONField(name = "account_name")
        @ApiModelProperty(value = "会员淘宝昵称")
        private String accountName;

        /** 备案时间 */
        @JSONField(name = "create_date")
        @ApiModelProperty(value = "备案时间")
        private String createDate;

        /** 会员关系 ID（sid） */
        @JSONField(name = "special_id")
        @ApiModelProperty(value = "会员关系ID（sid）")
        private Long specialId;

        /** 推广位 PID */
        @JSONField(name = "root_pid")
        @ApiModelProperty(value = "推广位PID")
        private String rootPid;
    }
}
