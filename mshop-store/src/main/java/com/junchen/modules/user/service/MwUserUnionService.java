/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserUnion;
import com.junchen.modules.user.service.dto.WechatUserDto;

import java.util.List;

/**
* @author huangyu
* @date 2023-05-12
*/
public interface MwUserUnionService extends BaseService<MwUserUnion>{
    List<MwUserUnion> queryAllContainsChannelId();

    MwUserUnion getOne(Long uid);
    MwUserUnion getByOpenId(String openId);
    void save(Long uid, WechatUserDto wechatUserDto);

    void update(MwUserUnion userUnion, WechatUserDto wechatUserDto);

    void remove(Long uid);

    List<MwUserUnion> getByUid(Long uid);

    List<MwUserUnion> listByTbPid(List<String> pids);


    /**
     * 增加淘礼金领取次数
     * @param uid uid
     */
    void incTljCount(Long uid);

}
