package com.junchen.modules.auth.param;

import com.junchen.modules.user.domain.MwUser;
import lombok.Data;

/**
 * @ClassName LoginParam
 * @author huangyu
 * @Date 2020/01/15
 **/
@Data
public class WechatLoginParam {

    private boolean register;

    private String openId;

    private MwUser user;
}
