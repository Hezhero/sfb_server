/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.shop.service;
import com.junchen.common.service.BaseService;
import com.junchen.modules.shop.domain.MwAppVersion;
import com.junchen.modules.shop.service.dto.MwAppVersionDto;
import com.junchen.modules.shop.service.dto.MwAppVersionQueryCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.junchen.domain.PageResult;
/**
* @author lioncity
* @date 2020-12-09
*/
public interface MwAppVersionService extends BaseService<MwAppVersion>{

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<MwAppVersionDto>  queryAll(MwAppVersionQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<MWAppVersionDto>
    */
    List<MwAppVersion> queryAll(MwAppVersionQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<MwAppVersionDto> all, HttpServletResponse response) throws IOException;
}
