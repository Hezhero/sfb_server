/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.mailvor.api.MshopException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@Data
public class DataokeConfig {
    @Value("${dataoke.key}")
    private String key;
    @Value("${dataoke.secret}")
    private String secret;

    /** 连接超时（毫秒），与 DataokeApiClient 主链路保持一致 */
    @Value("${dataoke.connect-timeout:5000}")
    private int connectTimeout;
    /** 读取超时（毫秒），与 DataokeApiClient 主链路保持一致 */
    @Value("${dataoke.read-timeout:15000}")
    private int readTimeout;

    /**
     * 启动自检：配置缺失时不直接崩启动，但把警告打到日志，
     * 首次调用时由 {@link com.mailvor.modules.tk.util.DataokeApiClient} 抛明确业务异常。
     * 密钥只输出长度与首尾各 4 位，不打印明文。
     */
    @PostConstruct
    public void logStatus() {
        boolean keyOk = key != null && !key.trim().isEmpty();
        boolean secretOk = secret != null && !secret.trim().isEmpty();
        if (!keyOk || !secretOk) {
            log.warn("大淘客配置不完整：key={}, secret={}，调用大淘客接口将返回明确业务错误",
                    keyOk ? mask(key) : "未配置", secretOk ? mask(secret) : "未配置");
            return;
        }
        log.info("大淘客配置就绪：key={}, secret={}, connectTimeout={}ms, readTimeout={}ms",
                mask(key), mask(secret), connectTimeout, readTimeout);
    }

    /** 首调用前的可用性检查：供 DataokeService 在发请求前调用，抛业务异常而非 NPE/JSONException */
    public void requireConfigured() {
        if (key == null || key.trim().isEmpty()) {
            throw new MshopException("大淘客 appKey 未配置，请检查 dataoke.key");
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new MshopException("大淘客 secret 未配置，请检查 dataoke.secret");
        }
    }

    private static String mask(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未配置";
        }
        String v = value.trim();
        if (v.length() <= 8) {
            return "****(长度" + v.length() + ")";
        }
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4) + "(长度" + v.length() + ")";
    }

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        RestTemplate restTemplate = new RestTemplate(factory);

        //换上fastjson
        List<HttpMessageConverter<?>> httpMessageConverterList= restTemplate.getMessageConverters();
//        Iterator<HttpMessageConverter<?>> iterator=httpMessageConverterList.iterator();
//        if(iterator.hasNext()){
//            HttpMessageConverter<?> converter=iterator.next();
//            //原有的String是ISO-8859-1编码 去掉
//            if(converter instanceof StringHttpMessageConverter){
//                iterator.remove();
//            }
//
//            //由于系统中默认有jackson 在转换json时自动会启用  但是我们不想使用它 可以直接移除或者将fastjson放在首位
//            /*if(converter instanceof GsonHttpMessageConverter || converter instanceof MappingJackson2HttpMessageConverter){
//                iterator.remove();
//            }*/
//
//        }
//        httpMessageConverterList.add(new StringHttpMessageConverter(Charset.forName("utf-8")));
        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.DisableCircularReferenceDetect);

        fastJsonHttpMessageConverter.setFastJsonConfig(fastJsonConfig);
//        httpMessageConverterList.add(0,fastJsonHttpMessageConverter);
        return restTemplate;
    }
    @Bean
    public ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        //ms
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        return factory;
    }
}
