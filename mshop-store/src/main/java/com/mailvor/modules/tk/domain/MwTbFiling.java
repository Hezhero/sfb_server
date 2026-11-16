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

import java.util.Date;

/**
 * 推客淘宝渠道备案信息（工单 03）。
 *
 * <p>淘宝联盟要求推广者与渠道方建立备案关系，未备案的推广行为无法归因，
 * 佣金从源头就断。每个推客（B 端用户）在这里保存一条备案记录，
 * 用于转链/佣金前置校验，也是后续结算争议的凭据。</p>
 *
 * <p>与 {@code mw_user_union} 的区别：后者是用户与当前支付/联盟账号的绑定关系
 * （含 {@code tb_pid}），而本表专门承载备案这一件事的状态与时间线，
 * 状态变更另有 {@link MwTbFilingLog} 留痕。</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mw_tb_filing")
@ApiModel(value = "MwTbFiling对象", description = "推客淘宝渠道备案信息")
public class MwTbFiling extends BaseDomain {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "推客用户uid")
    private Long uid;

    @ApiModelProperty(value = "备案使用的渠道推广位pid")
    private String tbPid;

    /** 与上游 relation_status 对齐：1-已备案 0-未备案 */
    @ApiModelProperty(value = "备案状态：1已备案 0未备案")
    private Integer relationStatus;

    @ApiModelProperty(value = "备案完成时间")
    private Date filedTime;

    @ApiModelProperty(value = "备案授权链接")
    private String relationUrl;

    @ApiModelProperty(value = "备注")
    private String remark;

    /** 是否已备案；null 视为未备案 */
    public boolean isFiled() {
        return relationStatus != null && relationStatus == 1;
    }
}
