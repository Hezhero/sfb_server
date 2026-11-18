/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenTbOrder;
import com.junchen.modules.tk.service.JunchenTbOrderService;
import com.junchen.modules.tk.service.dto.JunchenTbOrderDto;
import com.junchen.modules.tk.service.dto.JunchenTbOrderQueryCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
* @date 2022-08-29
*/
@AllArgsConstructor
@Api(tags = "商城：淘宝订单管理")
@RestController
@RequestMapping("/api/junchenTbOrder")
public class JunchenTbOrderController {

    private final JunchenTbOrderService junchenTbOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenTbOrder:list')")
    public void download(HttpServletResponse response, JunchenTbOrderQueryCriteria criteria) throws IOException {
        junchenTbOrderService.download(generator.convert(junchenTbOrderService.queryAll(criteria), JunchenTbOrderDto.class), response);
    }

    @GetMapping
    @Log("查询tbOrder")
    @ApiOperation("查询tbOrder")
    @PreAuthorize("@el.check('admin','junchenTbOrder:list')")
    public ResponseEntity<PageResult<JunchenTbOrderDto>> getJunchenTbOrders(JunchenTbOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(junchenTbOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增tbOrder")
    @ApiOperation("新增tbOrder")
    @PreAuthorize("@el.check('admin','junchenTbOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenTbOrder resources){
        return new ResponseEntity<>(junchenTbOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改tbOrder")
    @ApiOperation("修改tbOrder")
    @PreAuthorize("@el.check('admin','junchenTbOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenTbOrder resources){
        junchenTbOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除tbOrder")
    @ApiOperation("删除tbOrder")
    @PreAuthorize("@el.check('admin','junchenTbOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody Long[] ids) {
        Arrays.asList(ids).forEach(id->{
            junchenTbOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
