/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.user.rest;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.junchen.api.ApiResult;
import com.junchen.api.MshopException;
import com.junchen.common.bean.LocalUser;
import com.junchen.common.interceptor.AuthCheck;
import com.junchen.constant.ShopConstants;
import com.junchen.enums.PlatformEnum;
import com.junchen.enums.ShopCommonEnum;
import com.junchen.modules.shop.domain.MwSystemGroupData;
import com.junchen.modules.shop.service.MwSystemConfigService;
import com.junchen.modules.shop.service.MwSystemGroupDataService;
import com.junchen.modules.shop.service.dto.MwSystemGroupDataQueryCriteria;
import com.junchen.modules.user.domain.MwUserLevel;
import com.junchen.modules.user.dto.UserRechargeDTO;
import com.junchen.modules.user.service.MwSystemUserLevelService;
import com.junchen.modules.user.service.MwSystemUserTaskService;
import com.junchen.modules.user.service.MwUserLevelService;
import com.junchen.modules.user.service.dto.UserLevelDto;
import com.junchen.modules.user.service.dto.UserMultiLevelDto;
import com.junchen.modules.user.vo.MwSystemUserLevelQueryVo;
import com.junchen.utils.OrderUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.junchen.config.PayConfig.PAY_NAME;

/**
 * <p>
 * 用户等级 前端控制器
 * </p>
 *
 * @author huangyu
 * @since 2019-12-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(value = "用户等级", tags = "用户:用户等级")
public class UserLevelController {

    private final MwUserLevelService userLevelService;
    private final MwSystemUserLevelService systemUserLevelService;
    private final MwSystemUserTaskService systemUserTaskService;
    private final MwSystemGroupDataService systemGroupDataService;
    private final MwSystemConfigService systemConfigService;

    /**
     * 会员等级
     */
    @AuthCheck
    @GetMapping("/user/level")
    @ApiOperation(value = "会员等级",notes = "会员等级")
    public ApiResult<Object> getUserLevel(){
        Long uid = LocalUser.getUser().getUid();
        MwUserLevel userLevel = userLevelService.getUserLevel(uid, null);
        Integer discount = 33;
        if(userLevel != null) {
            discount = userLevel.getDiscount();
        }
        return ApiResult.ok(discount);
    }
    /**
    * 会员等级列表
     * type null或0=加盟 2=月卡
    */
    @AuthCheck
    @GetMapping("/user/level/grade")
    @ApiOperation(value = "会员等级列表",notes = "会员等级列表")
    public ApiResult<Object> getLevelInfo(){
        //获取所有充值方案
        MwSystemGroupDataQueryCriteria queryCriteria = new MwSystemGroupDataQueryCriteria();
        queryCriteria.setGroupName(ShopConstants.MSHOP_RECHARGE_PRICE_WAYS);
        queryCriteria.setStatus(ShopCommonEnum.IS_STATUS_1.getValue());
        List<MwSystemGroupData> mwSystemGroupDataList = systemGroupDataService.queryAll(queryCriteria);
        Map<Integer, String> rechargeMap = mwSystemGroupDataList.stream()
                .map(mwSystemGroupData -> JSON.parseObject(mwSystemGroupData.getValue(), UserRechargeDTO.class))
                .collect(Collectors.toMap(UserRechargeDTO::getId, UserRechargeDTO::getPrice));

        UserLevelDto userLevelDto = systemUserLevelService.getLevelInfo(null);
        List<MwSystemUserLevelQueryVo> list = new ArrayList<>(5);
        List<MwSystemUserLevelQueryVo> jdList = new ArrayList<>(5);
        List<MwSystemUserLevelQueryVo> pddList = new ArrayList<>(5);
        List<MwSystemUserLevelQueryVo> dyList = new ArrayList<>(5);
        List<MwSystemUserLevelQueryVo> vipList = new ArrayList<>(5);

        userLevelDto.getList().stream().forEach(levelDto->{
            String priceStr = rechargeMap.get(Integer.parseInt(levelDto.getRechargeId()));
            if(priceStr == null) {
                levelDto.setMoney(BigDecimal.ZERO);
            } else {
                levelDto.setMoney(BigDecimal.valueOf(Double.parseDouble(priceStr)));
            }
            levelDto.setIosProductId(OrderUtil.getIosProductId(levelDto.getType(), levelDto.getGrade()));
            if(PlatformEnum.TB.getValue().equals(levelDto.getType())) {
                list.add(levelDto);
            }else if(PlatformEnum.JD.getValue().equals(levelDto.getType())) {
                jdList.add(levelDto);
            }else if(PlatformEnum.PDD.getValue().equals(levelDto.getType())) {
                pddList.add(levelDto);
            }else if(PlatformEnum.DY.getValue().equals(levelDto.getType())) {
                dyList.add(levelDto);
            }else if(PlatformEnum.VIP.getValue().equals(levelDto.getType())) {
                vipList.add(levelDto);
            }
        });
        UserMultiLevelDto multiLevelDto = new UserMultiLevelDto();

        List multiList = new ArrayList();
        if(!list.isEmpty()) {
            Map map = new HashMap<>(2);
            if("tsq".equals(PAY_NAME)) {
                map.put("name", "淘店");
            } else {
                map.put("name", "淘星选");
            }
            map.put("key", "level");
            map.put("expKey", "expired");
            map.put("platform", "tb");
            map.put("list", list);
            multiList.add(map);
        }
        if(!jdList.isEmpty()) {
            Map map = new HashMap<>(2);
            if("tsq".equals(PAY_NAME)) {
                map.put("name", "京店");
            } else {
                map.put("name", "京星选");
            }
            map.put("key", "levelJd");
            map.put("expKey", "expiredJd");
            map.put("platform", "jd");
            map.put("list", jdList);
            multiList.add(map);
        }
        if(!pddList.isEmpty()) {
            Map map = new HashMap<>(2);
            if("tsq".equals(PAY_NAME)) {
                map.put("name", "多店");
            } else {
                map.put("name", "多星选");
            }
            map.put("key", "levelPdd");
            map.put("expKey", "expiredPdd");
            map.put("platform", "pdd");
            map.put("list", pddList);
            multiList.add(map);
        }
        if(!dyList.isEmpty()) {
            Map map = new HashMap<>(2);
            if("tsq".equals(PAY_NAME)) {
                map.put("name", "抖店");
            } else {
                map.put("name", "抖星选");
            }
            map.put("key", "levelDy");
            map.put("expKey", "expiredDy");
            map.put("platform", "dy");
            map.put("list", dyList);
            multiList.add(map);
        }
        if(!vipList.isEmpty()) {
            Map map = new HashMap<>(2);
            if("tsq".equals(PAY_NAME)) {
                map.put("name", "唯店");
            } else {
                map.put("name", "唯星选");
            }
            map.put("key", "levelVip");
            map.put("expKey", "expiredVip");
            map.put("platform", "vip");
            map.put("list", vipList);
            multiList.add(map);
        }
        multiLevelDto.setMultiList(multiList);

        return ApiResult.ok(multiLevelDto);
    }

    /**
     * 获取等级任务
     */
    @AuthCheck
    @GetMapping("/user/level/task/{id}")
    @ApiOperation(value = "获取等级任务",notes = "获取等级任务")
    public ApiResult<Object> getTask(@PathVariable String id){
        if(StrUtil.isBlank(id) || !NumberUtil.isNumber(id)){
            throw new MshopException("参数非法");
        }
        Long uid = LocalUser.getUser().getUid();
        return ApiResult.ok(systemUserTaskService.getTaskList(Integer.valueOf(id),uid));
    }

}

