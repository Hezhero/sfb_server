/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.order.service.impl;

import com.junchen.common.service.impl.BaseServiceImpl;
import com.junchen.common.utils.QueryHelpPlus;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.order.domain.MwStoreOrderStatus;
import com.junchen.modules.order.service.MwStoreOrderStatusService;
import com.junchen.modules.order.service.dto.MwStoreOrderStatusDto;
import com.junchen.modules.order.service.dto.MwStoreOrderStatusQueryCriteria;
import com.junchen.modules.order.service.mapper.StoreOrderStatusMapper;
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
* @author huangyu
* @date 2020-05-12
*/
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MwStoreOrderStatusServiceImpl extends BaseServiceImpl<StoreOrderStatusMapper, MwStoreOrderStatus> implements MwStoreOrderStatusService {

    private final IGenerator generator;


    /**
     * 添加订单操作记录
     * @param oid 订单id
     * @param changetype 操作状态
     * @param changeMessage 操作内容
     */
    @Override
    public void create(Long oid, String changetype, String changeMessage) {
        MwStoreOrderStatus storeOrderStatus = new MwStoreOrderStatus();
        storeOrderStatus.setOid(oid);
        storeOrderStatus.setChangeType(changetype);
        storeOrderStatus.setChangeMessage(changeMessage);
        this.baseMapper.insert(storeOrderStatus);
    }



    @Override
    //@Cacheable
    public Map<String, Object> queryAll(MwStoreOrderStatusQueryCriteria criteria, Pageable pageable) {
        getPage(pageable);
        PageInfo<MwStoreOrderStatus> page = new PageInfo<>(queryAll(criteria));
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("content", generator.convert(page.getList(), MwStoreOrderStatusDto.class));
        map.put("totalElements", page.getTotal());
        return map;
    }


    @Override
    //@Cacheable
    public List<MwStoreOrderStatus> queryAll(MwStoreOrderStatusQueryCriteria criteria){
        return baseMapper.selectList(QueryHelpPlus.getPredicate(MwStoreOrderStatus.class, criteria));
    }


    @Override
    public void download(List<MwStoreOrderStatusDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MwStoreOrderStatusDto mwStoreOrderStatus : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单id", mwStoreOrderStatus.getOid());
            map.put("操作类型", mwStoreOrderStatus.getChangeType());
            map.put("操作备注", mwStoreOrderStatus.getChangeMessage());
            map.put("操作时间", mwStoreOrderStatus.getChangeTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
