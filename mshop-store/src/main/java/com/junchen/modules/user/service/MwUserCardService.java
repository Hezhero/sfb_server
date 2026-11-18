/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.user.domain.MwUserCard;
import com.junchen.modules.user.service.dto.MwUserCardQueryCriteria;
import com.junchen.modules.user.vo.MwUserCardQueryVo;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwUserCardService extends BaseService<MwUserCard>{
    MwUserCardQueryVo getUserCardById(Long uid);
    MwUserCard getByCardNo(String cardNo);
    Map<String, Object> queryAll(MwUserCardQueryCriteria criteria, Pageable pageable);

    void clearSignature(Long uid);

    String generateContract(Long uid, MwUserCard card) throws Exception;
    String generateContract(Long uid, MwUserCard card, boolean convert) throws Exception;
    void cleanCard(Long uid);
    void cleanCardBank(Long uid);
    void cleanCardPhone(Long uid);

    void re(String id) throws Exception;
    void convertContract(Long uid);
}
