/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.annotation.*;
import com.junchen.common.handler.PgJsonbTypeHandler;
import com.junchen.domain.BaseDeleteDomain;
import com.junchen.modules.user.service.dto.TljDataDto;
import com.junchen.modules.user.service.dto.WechatUserDto;
import lombok.*;
import lombok.experimental.Accessors;

/**
* @author huangyu
* @date 2020-05-12
*/

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString
@TableName(value = "mw_user_union",autoResultMap = true)
public class MwUserUnion extends BaseDeleteDomain {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 用户id */
    private Long uid;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String openId;

    /**
     * 名称 对应yml中pay.name
     * */
    @TableField("biz_name")
    private String name;


    @TableField(typeHandler = PgJsonbTypeHandler.class,updateStrategy = FieldStrategy.IGNORED)
    private WechatUserDto wxProfile;


    /**淘宝渠道id， 非pid*/
    private String tbPid;
    /**淘礼金领取次数*/
    private Integer tljCount;
    @TableField(typeHandler = PgJsonbTypeHandler.class,updateStrategy = FieldStrategy.IGNORED)
    private TljDataDto tljData;

    public void copy(MwUserUnion source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
