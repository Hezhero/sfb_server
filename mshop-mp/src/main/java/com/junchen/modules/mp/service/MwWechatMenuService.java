/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.mp.service;

import com.junchen.common.service.BaseService;
import com.junchen.modules.mp.service.dto.MwWechatMenuDto;
import com.junchen.modules.mp.service.dto.MwWechatMenuQueryCriteria;
import com.junchen.modules.mp.domain.MwWechatMenu;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* @author huangyu
* @date 2020-05-12
*/
public interface MwWechatMenuService extends BaseService<MwWechatMenu>{

/**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    Map<String,Object> queryAll(MwWechatMenuQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<MwWechatMenuDto>
    */
    List<MwWechatMenu> queryAll(MwWechatMenuQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<MwWechatMenuDto> all, HttpServletResponse response) throws IOException;

    Boolean isExist(String wechat_menus);
}
