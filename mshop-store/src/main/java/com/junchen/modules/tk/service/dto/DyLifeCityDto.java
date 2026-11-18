/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.dto;

import lombok.Data;

import java.io.Serializable;

/**
* @author shenji
* @date 2022-12-17
*/
@Data
public class DyLifeCityDto implements Serializable {
    private Integer code;

    private String msg;

    private DyLifeCityDataDto data;

}
