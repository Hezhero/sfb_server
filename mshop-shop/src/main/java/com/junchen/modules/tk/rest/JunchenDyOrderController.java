/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenDyOrder;
import com.junchen.modules.tk.service.JunchenDyOrderService;
import com.junchen.modules.tk.service.dto.JunchenDyOrderDto;
import com.junchen.modules.tk.service.dto.JunchenDyOrderQueryCriteria;
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
* @date 2022-09-07
*/
@AllArgsConstructor
@Api(tags = "商城：抖音订单管理")
@RestController
@RequestMapping("/api/junchenDyOrder")
public class JunchenDyOrderController {

    private final JunchenDyOrderService junchenDyOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenDyOrder:list')")
    public void download(HttpServletResponse response, JunchenDyOrderQueryCriteria criteria) throws IOException {
        junchenDyOrderService.download(generator.convert(junchenDyOrderService.queryAll(criteria), JunchenDyOrderDto.class), response);
    }

    @GetMapping
    @Log("查询dyOrder")
    @ApiOperation("查询dyOrder")
    @PreAuthorize("@el.check('admin','junchenDyOrder:list')")
    public ResponseEntity<PageResult<JunchenDyOrderDto>> getJunchenDyOrders(JunchenDyOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(junchenDyOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增dyOrder")
    @ApiOperation("新增dyOrder")
    @PreAuthorize("@el.check('admin','junchenDyOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenDyOrder resources){
        return new ResponseEntity<>(junchenDyOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改dyOrder")
    @ApiOperation("修改dyOrder")
    @PreAuthorize("@el.check('admin','junchenDyOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenDyOrder resources){
        junchenDyOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除dyOrder")
    @ApiOperation("删除dyOrder")
    @PreAuthorize("@el.check('admin','junchenDyOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody String[] ids) {
        Arrays.asList(ids).forEach(id->{
            junchenDyOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
