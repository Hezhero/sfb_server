/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.dto;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.*;

/**
 * @author huangyu
 * @date 2020-05-12
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCheckDTO {

    /** 用户id */
    private Long uid;


    /** 数量 */
    private Long orderCount;
    public void copy(OrderCheckDTO source) {
        BeanUtil.copyProperties(source, this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
