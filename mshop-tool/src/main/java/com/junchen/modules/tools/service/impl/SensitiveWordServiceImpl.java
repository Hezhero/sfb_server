/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tools.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.tools.domain.SensitiveWord;
import com.junchen.modules.tools.service.SensitiveWordService;
import com.junchen.modules.tools.service.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author chaijing
 * @date 2022-10-06
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SensitiveWordServiceImpl extends BaseServiceImpl<SensitiveWordMapper, SensitiveWord>  implements SensitiveWordService {
    private final IGenerator generator;

    public SensitiveWordServiceImpl(IGenerator generator) {
        this.generator = generator;
    }

    @Override
    public List<SensitiveWord> findAll() {
        return baseMapper.selectList(new LambdaQueryWrapper<>());
    }
}
