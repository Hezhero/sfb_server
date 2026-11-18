/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.template.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.template.domain.MwSystemCity;
import com.junchen.modules.template.service.dto.MwSystemCityDto;
import com.junchen.modules.template.service.dto.MwSystemCityQueryCriteria;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* @author mazhongjun
* @date 2020-06-29
*/
public interface MwSystemCityService extends BaseService<MwSystemCity>{

/**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    Map<String,Object> queryAll(MwSystemCityQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<MwSystemCityDto>
    */
    List<MwSystemCity> queryAll(MwSystemCityQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<MwSystemCityDto> all, HttpServletResponse response) throws IOException;
}
