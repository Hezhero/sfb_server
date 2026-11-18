/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;


import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserAddress;
import com.junchen.modules.user.param.AddressParam;
import com.junchen.modules.user.vo.MwUserAddressQueryVo;

import java.util.List;

/**
 * <p>
 * 用户地址表 服务类
 * </p>
 *
 * @author huangyu
 * @since 2019-10-28
 */
public interface MwUserAddressService extends BaseService<MwUserAddress> {

    /**
     * 设置默认地址
     * @param uid uid
     * @param addressId 地址id
     */
    void setDefault(Long uid,Long addressId);

    /**
     * 添加或者修改地址
     * @param uid uid
     * @param param AddressParam
     */
    Long addAndEdit(Long uid, AddressParam param);

    /**
     * 地址详情
     * @param id 地址id
     * @return MwUserAddressQueryVo
     */
    MwUserAddressQueryVo getDetail(Long id);

    /**
     * 获取用户地址
     * @param uid uid
     * @param page page
     * @param limit limit
     * @return List
     */
    List<MwUserAddressQueryVo> getList(Long uid, int page, int limit);

    /**
     * 获取默认地址
     * @param uid uid
     * @return MwUserAddress
     */
    MwUserAddress getUserDefaultAddress(Long uid);

}
