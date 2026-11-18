/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen;

import com.junchen.annotation.AnonymousAccess;
import com.junchen.utils.SpringContextHolder;
import com.binarywang.spring.starter.wxjava.miniapp.config.WxMaAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author huangyu
 * @date 2018/11/15 9:20:19
 */
@EnableAsync
@RestController
@SpringBootApplication(exclude = {WxMaAutoConfiguration.class})
@EnableTransactionManagement
@MapperScan(basePackages ={"com.junchen.modules.*.service.mapper", "com.junchen.config"})
public class AdminServer {

    public static void main(String[] args) {
        SpringApplication.run(AdminServer.class, args);
        System.out.println(
                        "                     **                              \n" +
                        "                     **                              \n" +
                        "                     **                              \n" +
                        "   * ** ***   ****   ** ***     ****   ** ***        \n"+
                        "   ** *****  **  **  **** **   **  **  ***  **       \n" +
                        "   *  **  *  *       **    *  **    *  **    *       \n" +
                        "   *  **  *  ***     **    *  **    ** **    *       \n" +
                        "   *  **  *    ****  **    *  **    ** **    *       \n" +
                        "   *  **  *       *  **    *  **    ** **    *       \n"+
                        "   *  **  *  *    *  **    *   **  **  **   **       \n" +
                        "   *  **  *  ******  **    *    ****   ******        \n" +
                        "                                       **            \n"+
                        "                                       **            \n"+
                        "                                       **            \n"+
                    "\n三盈会电商系统管理后台启动成功 \n官网：https://syh.hzhero.cn 提供技术支持ﾞ  \n");
    }

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    @Bean
    public ServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory fa = new TomcatServletWebServerFactory();
        fa.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "[]{}"));
        return fa;
    }

    /**
     * 访问首页提示
     * @return /
     */
    @GetMapping("/")
    @AnonymousAccess
    public String index() {
        return "Backend service started successfully";
    }
}
