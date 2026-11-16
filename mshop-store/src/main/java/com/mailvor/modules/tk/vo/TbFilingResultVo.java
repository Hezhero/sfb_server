package com.mailvor.modules.tk.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 推客备案操作结果（工单 03）。
 *
 * <p>同时承载「获取备案链接」与「查询备案状态」两种响应的公共字段，
 * 前端只需看 {@code filed} 与 {@code hint} 就能决定是否提示用户去备案。</p>
 */
@Data
@ApiModel(value = "TbFilingResultVo", description = "推客备案操作结果")
public class TbFilingResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "推客uid")
    private Long uid;

    @ApiModelProperty(value = "备案使用的渠道推广位pid")
    private String tbPid;

    @ApiModelProperty(value = "是否已备案")
    private boolean filed;

    @ApiModelProperty(value = "备案状态：1已备案 0未备案")
    private Integer relationStatus;

    @ApiModelProperty(value = "状态文案：已备案/未备案")
    private String statusText;

    @ApiModelProperty(value = "备案授权链接")
    private String relationUrl;

    @ApiModelProperty(value = "淘宝分配的专用推广位（备案成功后返回）")
    private String specialPid;

    @ApiModelProperty(value = "备案完成时间")
    private Date filedTime;

    @ApiModelProperty(value = "未备案时的引导提示；已备案为null")
    private String hint;
}
