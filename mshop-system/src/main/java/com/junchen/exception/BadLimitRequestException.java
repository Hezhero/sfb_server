/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.exception;

import com.junchen.api.ApiCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author Zheng Jie
 * @date 2018-11-23
 * 统一异常处理
 */
@Getter
public class BadLimitRequestException extends RuntimeException{

    private Integer status = ApiCode.BAD_LIMIT_EXCEPTION.getCode();

    public BadLimitRequestException(String msg){
        super(msg);
    }

    public BadLimitRequestException(HttpStatus status, String msg){
        super(msg);
        this.status = status.value();
    }
}
