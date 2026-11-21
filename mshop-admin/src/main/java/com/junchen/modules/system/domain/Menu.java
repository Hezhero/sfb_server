/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.system.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.junchen.domain.BaseDomain;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @author huangyu
* @date 2020-05-14
*/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_menu")
public class Menu extends BaseDomain {

    /** ID */
    @TableId
    private Long id;


    /** 是否外链 */
    private Boolean iFrame;


    /** 菜单名称 */
    @NotBlank(message = "请填写菜单名称")
    @TableField("biz_name")
    private String name;


    /** 组件 */
    private String component;


    /** 上级菜单ID */
    @NotNull(message = "上级菜单ID不能为空")
    private Long pid;


    /** 排序 */
    @NotNull(message = "排序不能为空")
    private Long sort;


    /** 图标 */
    private String icon;


    /** 链接地址 */
    private String path;


    /** 缓存 */
    @TableField("biz_cache")
    private Boolean cache;


    /** 是否隐藏 */
    private Boolean hidden;


    /** 组件名称 */
    private String componentName;




    /** 权限 */
    private String permission;


    /** 类型，目录、菜单、按钮 */
    @TableField("biz_type")
    private Integer type;


    public void copy(Menu source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
