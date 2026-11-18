package com.junchen;

import com.junchen.utils.SpringContextHolder;
import com.binarywang.spring.starter.wxjava.miniapp.config.WxMaAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author mazhongjun
 * @date 2019/10/1 9:20:19
 */
@EnableAsync
@EnableTransactionManagement
@EnableCaching
@MapperScan(basePackages ={"com.junchen.modules.*.service.mapper", "com.junchen.config"})
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class , WxMaAutoConfiguration.class})
public class AppServer {

    public static void main(String[] args) {
        SpringApplication.run(AppServer.class, args);

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
                        "\n三盈会电商系统移动端API启动成功 \n官网：https://syh.hzhero.cn 提供技术支持ﾞ  \n");
    }

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }
}
