/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.mp.service.dto;

import lombok.Data;

import java.io.Serializable;

/**
* @author huangyu
* @date 2020-05-12
*/
@Data
public class MwWechatMenuDto implements Serializable {

    private String key;

    /** 缓存数据 */
    private String result;

    /** 缓存时间 */
    private Integer addTime;
}
