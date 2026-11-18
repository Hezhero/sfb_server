/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.mp.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.dozer.service.IGenerator;
import com.junchen.exception.EntityExistException;
import com.junchen.modules.mp.service.mapper.WechatReplyMapper;
import com.junchen.modules.mp.domain.MwWechatReply;
import com.junchen.modules.mp.service.MwWechatReplyService;
import com.junchen.modules.mp.service.dto.MwWechatReplyDto;
import com.junchen.modules.mp.service.dto.MwWechatReplyQueryCriteria;
import com.junchen.utils.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

// 默认不使用缓存
//import org.springframework.cache.annotation.CacheConfig;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;

/**
* @author huangyu
* @date 2020-05-12
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "mwWechatReply")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwWechatReplyServiceImpl extends BaseServiceImpl<WechatReplyMapper, MwWechatReply> implements MwWechatReplyService {

    private final IGenerator generator;

    @Override
    //@Cacheable
    public Map<String, Object> queryAll(MwWechatReplyQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<MwWechatReply> page = new PageInfo<>(queryAll(criteria));
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("content", generator.convert(page.getList(), MwWechatReplyDto.class));
        map.put("totalElements", page.getTotal());
        return map;
    }


    @Override
    //@Cacheable
    public List<MwWechatReply> queryAll(MwWechatReplyQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(MwWechatReply.class, criteria));
    }


    @Override
    public void download(List<MwWechatReplyDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MwWechatReplyDto mwWechatReply : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("关键字", mwWechatReply.getKey());
            map.put("回复类型", mwWechatReply.getType());
            map.put("回复数据", mwWechatReply.getData());
            map.put("0=不可用  1 =可用", mwWechatReply.getStatus());
            map.put("是否隐藏", mwWechatReply.getHide());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public MwWechatReply isExist(String key) {
        MwWechatReply mwWechatReply = this.getOne(new LambdaQueryWrapper<MwWechatReply>()
                .eq(MwWechatReply::getKey,key));
        return mwWechatReply;
    }

    @Override
    public void create(MwWechatReply mwWechatReply) {
        if(this.isExist(mwWechatReply.getKey()) != null){
            throw new EntityExistException(MwWechatReply.class,"key",mwWechatReply.getKey());
        }
        this.save(mwWechatReply);
    }

    @Override
    public void upDate(MwWechatReply resources) {
        MwWechatReply mwWechatReply = this.getById(resources.getId());
        MwWechatReply mwWechatReply1;
        mwWechatReply1 = this.isExist(resources.getKey());
        if(mwWechatReply1 != null && !mwWechatReply1.getId().equals(mwWechatReply.getId())){
            throw new EntityExistException(MwWechatReply.class,"key",resources.getKey());
        }
        mwWechatReply.copy(resources);
        this.saveOrUpdate(mwWechatReply);
    }
}
