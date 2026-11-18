/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.shop.service.impl;

import com.junchen.modules.shop.domain.MwAppVersion;
import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.modules.shop.service.MwAppVersionService;
import com.junchen.modules.shop.service.dto.MwAppVersionDto;
import com.junchen.modules.shop.service.dto.MwAppVersionQueryCriteria;
import com.junchen.modules.shop.service.mapper.MwAppVersionMapper;
import lombok.AllArgsConstructor;
import com.junchen.dozer.service.IGenerator;
import com.github.pagehelper.PageInfo;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.utils.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
// 默认不使用缓存
//import org.springframework.cache.annotation.CacheConfig;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.junchen.domain.PageResult;
/**
* @author lioncity
* @date 2020-12-09
*/
@Service
@AllArgsConstructor
//@CacheConfig(cacheNames = "mwAppVersion")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwAppVersionServiceImpl extends BaseServiceImpl<MwAppVersionMapper, MwAppVersion> implements MwAppVersionService {

    private final IGenerator generator;

    @Override
    //@Cacheable
    public PageResult<MwAppVersionDto> queryAll(MwAppVersionQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<MwAppVersion> page = new PageInfo<>(queryAll(criteria));
        return generator.convertPageInfo(page, MwAppVersionDto.class);
    }


    @Override
    //@Cacheable
    public List<MwAppVersion> queryAll(MwAppVersionQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(MwAppVersion.class, criteria));
    }


    @Override
    public void download(List<MwAppVersionDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MwAppVersionDto mwAppVersion : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("是否删除",  mwAppVersion.getIsDel());
            map.put("创建时间", mwAppVersion.getCreateTime());
            map.put("更新",  mwAppVersion.getUpdateTime());
            map.put("安卓版本号", mwAppVersion.getAndroidVersion());
            map.put("iOS版本号", mwAppVersion.getIosVersion());
            map.put("版本名称", mwAppVersion.getVersionName());
            map.put("版本描述", mwAppVersion.getVersionInfo());
            map.put("安卓下载链接", mwAppVersion.getAndroidUrl());
            map.put("是否强制升级", mwAppVersion.getForceUpdate());
            map.put("iOS下载链接", mwAppVersion.getIosUrl());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
