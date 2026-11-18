/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserFeeLog;
import com.junchen.modules.user.domain.MwUserFeeLogOpt;

import java.util.List;
import java.util.Map;

/**
* @author huangyu
* @date 2023-05-12
*/
public interface MwUserFeeLogOptService extends BaseService<MwUserFeeLogOpt>{
    List<MwUserFeeLog> feeList(Long uid, Integer cid, Integer type);
    List<MwUserFeeLog> feeList(Long uid, Integer type);
    Map<String, MwUserFeeLog> userFeeDetail(Long uid, Integer cid, Integer type);
    int mysqlInsertOrUpdateBath(List list);
}
