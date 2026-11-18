/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenPddOrder;
import com.junchen.modules.tk.service.JunchenPddOrderService;
import com.junchen.modules.tk.service.dto.JunchenPddOrderDto;
import com.junchen.modules.tk.service.dto.JunchenPddOrderQueryCriteria;
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
* @author shenji
* @date 2022-09-06
*/
@AllArgsConstructor
@Api(tags = "商城：拼多多订单管理")
@RestController
@RequestMapping("/api/junchenPddOrder")
public class JunchenPddOrderController {

    private final JunchenPddOrderService junchenPddOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenPddOrder:list')")
    public void download(HttpServletResponse response, JunchenPddOrderQueryCriteria criteria) throws IOException {
        junchenPddOrderService.download(generator.convert(junchenPddOrderService.queryAll(criteria), JunchenPddOrderDto.class), response);
    }

    @GetMapping
    @Log("查询pddOrder")
    @ApiOperation("查询pddOrder")
    @PreAuthorize("@el.check('admin','junchenPddOrder:list')")
    public ResponseEntity<PageResult<JunchenPddOrderDto>> getJunchenPddOrders(JunchenPddOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(junchenPddOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增pddOrder")
    @ApiOperation("新增pddOrder")
    @PreAuthorize("@el.check('admin','junchenPddOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenPddOrder resources){
        return new ResponseEntity<>(junchenPddOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改pddOrder")
    @ApiOperation("修改pddOrder")
    @PreAuthorize("@el.check('admin','junchenPddOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenPddOrder resources){
        junchenPddOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除pddOrder")
    @ApiOperation("删除pddOrder")
    @PreAuthorize("@el.check('admin','junchenPddOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody String[] ids) {
        Arrays.asList(ids).forEach(id->{
            junchenPddOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
