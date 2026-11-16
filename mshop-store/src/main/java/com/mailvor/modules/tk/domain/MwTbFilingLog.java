package com.mailvor.modules.tk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mailvor.domain.BaseDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 推客备案状态变更记录（工单 03）。
 *
 * <p>每次备案状态发生变化（未备案 → 已备案，或反向）写一条。
 * 状态没变不写，避免推客每次查询都产生一条噪音。
 * 这张表是结算争议时的追溯依据：推客主张"我早就备案了"时，
 * 用它证明某笔订单发生时该推客处于什么状态。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mw_tb_filing_log")
@ApiModel(value = "MwTbFilingLog对象", description = "推客备案状态变更记录")
public class MwTbFilingLog extends BaseDomain {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "推客用户uid")
    private Long uid;

    @ApiModelProperty(value = "备案使用的渠道推广位pid")
    private String tbPid;

    @ApiModelProperty(value = "变更前状态：1已备案 0未备案，null表示首次记录")
    private Integer fromStatus;

    @ApiModelProperty(value = "变更后状态：1已备案 0未备案")
    private Integer relationStatus;

    @ApiModelProperty(value = "变更说明")
    private String remark;
}
