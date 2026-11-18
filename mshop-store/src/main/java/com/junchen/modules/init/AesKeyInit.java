package com.junchen.modules.init;

import com.junchen.modules.utils.AesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AesKeyInit {
    @Value("${aes.encryptKey}")
    private String aesKey;
    @PostConstruct
    public void init() {
        AesUtil.init(aesKey);
    }
}
