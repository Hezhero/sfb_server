/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenJdOrder;
import com.junchen.modules.tk.service.JunchenJdOrderService;
import com.junchen.modules.tk.service.dto.JunchenJdOrderDto;
import com.junchen.modules.tk.service.dto.JunchenJdOrderQueryCriteria;
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
* @date 2022-09-05
*/
@AllArgsConstructor
@Api(tags = "商城：京东订单管理")
@RestController
@RequestMapping("/api/junchenJdOrder")
public class JunchenJdOrderController {

    private final JunchenJdOrderService junchenJdOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenJdOrder:list')")
    public void download(HttpServletResponse response, JunchenJdOrderQueryCriteria criteria) throws IOException {
        junchenJdOrderService.download(generator.convert(junchenJdOrderService.queryAll(criteria), JunchenJdOrderDto.class), response);
    }

    @GetMapping
    @Log("查询jdOrder")
    @ApiOperation("查询jdOrder")
    @PreAuthorize("@el.check('admin','junchenJdOrder:list')")
    public ResponseEntity<PageResult<JunchenJdOrderDto>> getJunchenJdOrders(JunchenJdOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(junchenJdOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增jdOrder")
    @ApiOperation("新增jdOrder")
    @PreAuthorize("@el.check('admin','junchenJdOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenJdOrder resources){
        return new ResponseEntity<>(junchenJdOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改jdOrder")
    @ApiOperation("修改jdOrder")
    @PreAuthorize("@el.check('admin','junchenJdOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenJdOrder resources){
        junchenJdOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除jdOrder")
    @ApiOperation("删除jdOrder")
    @PreAuthorize("@el.check('admin','junchenJdOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody String[] ids) {
        Arrays.asList(ids).forEach(id->{
            junchenJdOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
