/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 *
 */
package com.junchen.modules.tk.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class PddConfig {
    @Value("${pdd.paramPrefix: sfbuid}")
    private String paramPrefix;

    @Value("${pdd.clientId}")
    private String clientId;
    @Value("${pdd.clientSecret}")
    private String clientSecret;

    @Value("${pdd.pid}")
    private String pid;


    @Value("${pdd.superRedPackage:0}")
    private Integer superRedPackage;

    public String getParam(Long uid) {
        return "{\"" + paramPrefix + "\":\""+uid + "\"}";
    }

}
