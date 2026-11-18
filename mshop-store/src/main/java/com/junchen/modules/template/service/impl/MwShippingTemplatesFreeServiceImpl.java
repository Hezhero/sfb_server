/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.template.service.impl;


import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.template.domain.MwShippingTemplatesFree;
import com.junchen.modules.template.service.MwShippingTemplatesFreeService;
import com.junchen.modules.template.service.dto.MwShippingTemplatesFreeDto;
import com.junchen.modules.template.service.dto.MwShippingTemplatesFreeQueryCriteria;
import com.junchen.modules.template.service.mapper.MwShippingTemplatesFreeMapper;
import com.junchen.utils.FileUtil;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
* @author mazhongjun
* @date 2020-06-29
*/
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwShippingTemplatesFreeServiceImpl extends BaseServiceImpl<MwShippingTemplatesFreeMapper, MwShippingTemplatesFree> implements MwShippingTemplatesFreeService {

    private final IGenerator generator;

    @Override
    //@Cacheable
    public Map<String, Object> queryAll(MwShippingTemplatesFreeQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<MwShippingTemplatesFree> page = new PageInfo<>(queryAll(criteria));
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("content", generator.convert(page.getList(), MwShippingTemplatesFreeDto.class));
        map.put("totalElements", page.getTotal());
        return map;
    }


    @Override
    //@Cacheable
    public List<MwShippingTemplatesFree> queryAll(MwShippingTemplatesFreeQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(MwShippingTemplatesFree.class, criteria));
    }


    @Override
    public void download(List<MwShippingTemplatesFreeDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MwShippingTemplatesFreeDto mwShippingTemplatesFree : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("省ID", mwShippingTemplatesFree.getProvinceId());
            map.put("模板ID", mwShippingTemplatesFree.getTempId());
            map.put("城市ID", mwShippingTemplatesFree.getCityId());
            map.put("包邮件数", mwShippingTemplatesFree.getNumber());
            map.put("包邮金额", mwShippingTemplatesFree.getPrice());
            map.put("计费方式", mwShippingTemplatesFree.getType());
            map.put("分组唯一值", mwShippingTemplatesFree.getUniqid());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
