/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.activity.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.activity.domain.MwUserExtract;
import com.junchen.modules.activity.param.UserExtParam;
import com.junchen.modules.activity.service.dto.MwExtractConfigDto;
import com.junchen.modules.activity.service.dto.MwExtractConfigParam;
import com.junchen.modules.activity.service.dto.MwUserExtractDto;
import com.junchen.modules.activity.service.dto.MwUserExtractQueryCriteria;
import com.junchen.modules.user.domain.MwUser;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* @author huangyu
* @date 2020-05-13
*/
public interface MwUserExtractService extends BaseService<MwUserExtract>{

    /**
     * 开始提现
     * @param userInfo 用户
     * @param param UserExtParam
     */
    void userExtract(MwUser userInfo, UserExtParam param,String ip);

    /**
     * 累计提现金额
     * @param uid uid
     * @return double
     */
    double extractSum(Long uid);


    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    Map<String,Object> queryAll(MwUserExtractQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<MwUserExtractDto>
    */
    List<MwUserExtract> queryAll(MwUserExtractQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<MwUserExtractDto> all, HttpServletResponse response) throws IOException;


    /**
     * 操作提现
     * @param resources MwUserExtract
     */
    void doExtract(MwUserExtract resources);


    void doExtracts(List<MwUserExtract> resources);

    Double totalExtractPrice(Integer type);

    void updateUnpaidAliCode(Long uid, String userId);

    void setExtractConfig(MwExtractConfigParam param);
    MwExtractConfigParam getExtractConfig();
}
