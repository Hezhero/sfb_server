/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.shop.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.junchen.domain.BaseDomain;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
* @author mazhongjun
* @date 2020-05-12
*/

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mw_material")
public class MwMaterial extends BaseDomain {

    /** PK */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;


    /** 创建者ID */
    private String createId;


    /** 类型1、图片；2、视频 */
    @TableField("biz_type")
    private String type;


    /** 分组ID */
    private String groupId;


    /** 素材名 */
    @TableField("biz_name")
    private String name;


    /** 素材链接 */
    private String url;


    public void copy(MwMaterial source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
