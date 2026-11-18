/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.canvas.service;
import com.junchen.common.service.BaseService;
import com.junchen.modules.canvas.domain.StoreCanvas;
import com.junchen.modules.canvas.service.dto.StoreCanvasDto;
import com.junchen.modules.canvas.service.dto.StoreCanvasQueryCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.junchen.domain.PageResult;
/**
* @author mshop
* @date 2021-02-01
*/
public interface StoreCanvasService  extends BaseService<StoreCanvas>{

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<StoreCanvasDto>  queryAll(StoreCanvasQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<StoreCanvasDto>
    */
    List<StoreCanvas> queryAll(StoreCanvasQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<StoreCanvasDto> all, HttpServletResponse response) throws IOException;
}
