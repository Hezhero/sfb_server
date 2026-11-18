/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.junchen.domain.BaseDeleteDomain;
import com.junchen.modules.user.service.dto.UserCardDto;
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
@TableName(value = "mw_user_card",autoResultMap = true)
public class MwUserCard extends BaseDeleteDomain {

    /** 用户id */
    @TableId(value = "uid", type = IdType.NONE)
    private Long uid;

    /** 身份证号码 */
    private String cardNoEnc;
    private String cardNoMd5;
    private String bankNoEnc;
    private String bankNoMd5;
    /** 姓名 */
    private String cardName;
    /** 手机号 */
    private String phone;
    /** 身份证过期时间 */
    private String cardExpired;

    /** 身份证人像面地址 */
    private String cardFPath;
    /** 身份证国徽面地址 */
    private String cardBPath;
    /** 合同地址 */
    private String contractPath;
    private String contractPathNow;

    /** 人脸地址 */
    private String facePath;

    @TableField(typeHandler = FastjsonTypeHandler.class)
    private UserCardDto cards;
    /** 签字地址 */
    private String signaturePath;
    public void copy(MwUserCard source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
