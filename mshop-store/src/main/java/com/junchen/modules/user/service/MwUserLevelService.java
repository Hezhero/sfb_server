/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserLevel;


/**
 * <p>
 * 用户等级记录表 服务类
 * </p>
 *
 * @author huangyu
 * @since 2019-12-06
 */
public interface MwUserLevelService extends BaseService<MwUserLevel> {


    /**
     * 检查是否能成为会员
     * @param uid 用户id
     */
    boolean setLevelComplete(Long uid);

    //UserLevelInfoDto getUserLevelInfo(int id);

    /**
     * 获取当前用户会员等级返回当前用户等级
     * @param uid uid
     * @param grade 用户级别
     * @return MwUserLevel
     */
    MwUserLevel getUserLevel(Long uid, Integer grade);

    void setUserLevel(Long uid, int levelId, String platform);

}
