/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tools.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.tools.service.dto.QiniuConfigDto;
import com.junchen.modules.tools.service.dto.QiniuQueryCriteria;
import com.junchen.modules.tools.domain.QiniuConfig;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* @author huangyu
* @date 2020-05-13
*/
public interface QiniuConfigService  extends BaseService<QiniuConfig>{

/**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    Map<String,Object> queryAll(QiniuQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<QiniuConfigDto>
    */
    List<QiniuConfig> queryAll(QiniuQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<QiniuConfigDto> all, HttpServletResponse response) throws IOException;


    void update(String type);
}
