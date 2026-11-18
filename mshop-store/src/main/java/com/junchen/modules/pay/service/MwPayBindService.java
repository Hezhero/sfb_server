/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.pay.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.pay.domain.MwPayBind;

/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwPayBindService extends BaseService<MwPayBind>{
    MwPayBind findBindChannel(Long uid, Integer payType);

    void bindChannel(Long uid , Long channelId, Integer payType);

    void rebindChannel(Long uid , Long channelId, Integer payType);
}
