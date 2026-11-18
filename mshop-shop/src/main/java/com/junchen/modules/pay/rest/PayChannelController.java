/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.pay.rest;

import com.junchen.api.MshopException;
import com.junchen.constant.ShopConstants;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.aop.ForbidSubmit;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.pay.domain.MwPayChannel;
import com.junchen.modules.pay.config.PayCertificateProfiles;
import com.junchen.modules.pay.dto.PayChannelQueryCriteria;
import com.junchen.modules.pay.rest.param.PayChannelEditParam;
import com.junchen.modules.pay.rest.param.PayChannelParam;
import com.junchen.modules.pay.rest.param.PayConfigParam;
import com.junchen.modules.pay.service.MwPayChannelService;
import com.junchen.modules.pay.service.MwPayCompanyService;
import com.junchen.modules.shop.service.MwSystemConfigService;
import com.junchen.modules.shop.service.dto.PayConfigDto;
import com.junchen.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @author huangyu
 * @date 2023-04-11
 */
@Api(tags = "商城：:支付通道管理")
@RestController
@RequestMapping("api")
public class PayChannelController {

    @Resource
    private MwPayChannelService payConfigService;

    @Resource
    private MwPayCompanyService payCompanyService;

    @Resource
    private IGenerator generator;

    @Resource
    private MwSystemConfigService systemConfigService;

    @Resource
    private PayCertificateProfiles certificateProfiles;

    @GetMapping("/payset/{id}/certificate")
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_EDIT')")
    public ResponseEntity certificateMetadata(@PathVariable Long id) {
        MwPayChannel channel = payConfigService.getById(id);
        if (channel == null) throw new MshopException("通道不存在");
        return ResponseEntity.ok(certificateProfiles.metadata(channel.getChannelKey(), channel.getCertProfileEnc()));
    }

    @Log("查询支付通道")
    @ApiOperation(value = "查询支付通道")
    @GetMapping(value = "/payset")
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_SELECT')")
    public ResponseEntity getPayChannelList(PayChannelQueryCriteria criteria,
                                            Pageable pageable) {
        return new ResponseEntity<>(payConfigService.queryAll(criteria, pageable), HttpStatus.OK);
    }

    @ForbidSubmit
    @Log("新增支付通道")
    @ApiOperation(value = "新增支付通道")
    @PostMapping(value = "/payset")
    @CacheEvict(cacheNames = ShopConstants.MSHOP_REDIS_INDEX_KEY, allEntries = true)
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_CREATE')")
    public ResponseEntity create(@Valid @RequestBody PayChannelParam param) {
        boolean existCompany = payCompanyService.checkCompany(param.getCompanyId());
        if(!existCompany) {
            throw new MshopException("主体id不存在");
        }

        MwPayChannel payChannel = generator.convert(param, MwPayChannel.class);
        if (StringUtils.isBlank(param.getCertProfileE())) throw new MshopException("请配置支付证书信息");
        payChannel.setCertProfileEnc(certificateProfiles.prepare(param.getChannelKey(), null,
                param.getCertProfileE(), param.isMergeCertProfile()));
        return new ResponseEntity<>(payConfigService.save(payChannel), HttpStatus.CREATED);
    }

    @ForbidSubmit
    @Log("修改支付通道")
    @ApiOperation(value = "修改支付通道")
    @PutMapping(value = "/payset")
    @CacheEvict(cacheNames = ShopConstants.MSHOP_REDIS_INDEX_KEY, allEntries = true)
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_EDIT')")
    public ResponseEntity update(@Valid @RequestBody PayChannelEditParam param) {
        boolean existCompany = payCompanyService.checkCompany(param.getCompanyId());
        if(!existCompany) {
            throw new MshopException("主体id不存在");
        }
        MwPayChannel payChannel = generator.convert(param, MwPayChannel.class);
        MwPayChannel existing = payConfigService.getById(param.getId());
        if (existing == null) throw new MshopException("通道不存在");
        if (!existing.getChannelKey().equals(param.getChannelKey()) && StringUtils.isBlank(param.getCertProfileE())) {
            throw new MshopException("更换通道商后请重新配置支付证书");
        }
        byte[] original = existing.getChannelKey().equals(param.getChannelKey()) ? existing.getCertProfileEnc() : null;
        payChannel.setCertProfileEnc(certificateProfiles.prepare(param.getChannelKey(), original,
                param.getCertProfileE(), param.isMergeCertProfile()));

        payConfigService.saveOrUpdate(payChannel);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @ForbidSubmit
    @Log("删除支付通道")
    @ApiOperation(value = "删除支付通道")
    @DeleteMapping(value = "/payset/{id}")
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_DELETE')")
    public ResponseEntity delete(@PathVariable Long id) {
        payConfigService.removeById(id);
        return new ResponseEntity(HttpStatus.OK);
    }
    @Log("设置APP支付开关")
    @ApiOperation(value = "设置APP支付开关")
    @PostMapping(value = "/pay/config")
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_DELETE')")
    public ResponseEntity setPayConfig(@Valid @RequestBody PayConfigParam param){
        systemConfigService.setAppPayConfig(generator.convert(param, PayConfigDto.class));
        return new ResponseEntity(HttpStatus.OK);

    }
    @Log("获取APP支付开关")
    @ApiOperation(value = "获取APP支付开关")
    @GetMapping(value = "/pay/config")
    @PreAuthorize("hasAnyRole('admin','PAYSET_ALL','PAYSET_DELETE')")
    public ResponseEntity setPayConfig(){
        return new ResponseEntity(systemConfigService.getAppPayConfig(), HttpStatus.OK);

    }

}
