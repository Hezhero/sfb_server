/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.pay.service.impl;

import com.github.pagehelper.PageInfo;
import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.pay.service.MwPayCompanyService;
import com.junchen.modules.pay.domain.MwPayChannel;
import com.junchen.modules.pay.domain.MwPayCompany;
import com.junchen.modules.pay.dto.PayCompanyDto;
import com.junchen.modules.pay.dto.PayCompanyQueryCriteria;
import com.junchen.modules.pay.service.mapper.PayCompanyMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
* @author wangke
* @date 2020-05-12
*/
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwPayCompanyServiceImpl extends BaseServiceImpl<PayCompanyMapper, MwPayCompany> implements MwPayCompanyService {

    private final IGenerator generator;

    @Override
    //@Cacheable
    public Map<String, Object> queryAll(PayCompanyQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<MwPayCompany> page = new PageInfo<>(queryAll(criteria));
        Map<String, Object> map = new LinkedHashMap<>(2);
        List<PayCompanyDto> payChannelDtos = generator.convert(page.getList(), PayCompanyDto.class);

        map.put("content",payChannelDtos);
        map.put("totalElements",page.getTotal());
        return map;
    }

    @Override
    //@Cacheable
    public List<MwPayCompany> queryAll(PayCompanyQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(MwPayChannel.class, criteria));
    }

    @Override
    public boolean checkCompany(Long id) {
        return getById(id) != null;
    }
}
