/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.junchen.domain.BaseDomain;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
* @author huangyu
* @date 2020-05-12
*/

@Data
@TableName("mw_system_user_task")
public class MwSystemUserTask extends BaseDomain {

    @TableId
    private Integer id;


    /** 任务名称 */
    @TableField("biz_name")
    private String name;


    /** 配置原名 */
    private String realName;


    /** 任务类型 fans=粉丝数量 order=订单数量 sign=签到*/
    private String taskType;

    /** 奖励类型 0=金额 1=积分*/
//    private Integer rewardType;
    /** 奖励数值 */
//    private Double reward;

    /** 限定数 */
    private Integer number;


    /** 等级id 未用*/
    private Integer levelId;


    /** 排序 */
    private Integer sort;


    /** 是否显示 */
    private Boolean isShow;


    /** 是否务必达成任务,1务必达成,0=满足其一 未用*/
    private Boolean isMust;


    /** 任务说明 */
    private String illustrate;


    public void copy(MwSystemUserTask source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
