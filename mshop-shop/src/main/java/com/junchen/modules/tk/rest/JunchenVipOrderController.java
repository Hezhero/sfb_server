/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenVipOrder;
import com.junchen.modules.tk.service.JunchenVipOrderService;
import com.junchen.modules.tk.service.dto.JunchenVipOrderDto;
import com.junchen.modules.tk.service.dto.JunchenVipOrderQueryCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
/**
* @author wangjun
* @date 2022-09-07
*/
@AllArgsConstructor
@Api(tags = "商城：唯品会订单管理")
@RestController
@RequestMapping("/api/junchenVipOrder")
public class JunchenVipOrderController {

    private final JunchenVipOrderService junchenVipOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenVipOrder:list')")
    public void download(HttpServletResponse response, JunchenVipOrderQueryCriteria criteria) throws IOException {
        junchenVipOrderService.download(generator.convert(junchenVipOrderService.queryAll(criteria), JunchenVipOrderDto.class), response);
    }

    @GetMapping
    @Log("查询vipOrder")
    @ApiOperation("查询vipOrder")
    @PreAuthorize("@el.check('admin','junchenVipOrder:list')")
    public ResponseEntity<PageResult<JunchenVipOrderDto>> getJunchenVipOrders(JunchenVipOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(junchenVipOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增vipOrder")
    @ApiOperation("新增vipOrder")
    @PreAuthorize("@el.check('admin','junchenVipOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenVipOrder resources){
        return new ResponseEntity<>(junchenVipOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改vipOrder")
    @ApiOperation("修改vipOrder")
    @PreAuthorize("@el.check('admin','junchenVipOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenVipOrder resources){
        junchenVipOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除vipOrder")
    @ApiOperation("删除vipOrder")
    @PreAuthorize("@el.check('admin','junchenVipOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody String[] ids) {
        Arrays.asList(ids).forEach(id->{
            junchenVipOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
