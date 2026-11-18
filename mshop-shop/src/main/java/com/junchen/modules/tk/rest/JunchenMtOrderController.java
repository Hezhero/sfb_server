/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.rest;

import com.junchen.domain.PageResult;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.Log;
import com.junchen.modules.tk.domain.JunchenMtOrder;
import com.junchen.modules.tk.service.JunchenMtOrderService;
import com.junchen.modules.tk.service.dto.JunchenMtOrderQueryCriteria;
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
* @date 2022-08-29
*/
@AllArgsConstructor
@Api(tags = "商城：美团订单管理")
@RestController
@RequestMapping("/api/junchenMtOrder")
public class JunchenMtOrderController {

    private final JunchenMtOrderService mtOrderService;
    private final IGenerator generator;


    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('admin','junchenTbOrder:list')")
    public void download(HttpServletResponse response, JunchenMtOrderQueryCriteria criteria) throws IOException {
        mtOrderService.download(mtOrderService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询mtOrder")
    @ApiOperation("查询mtOrder")
    @PreAuthorize("@el.check('admin','junchenMtOrder:list')")
    public ResponseEntity<PageResult<JunchenMtOrder>> getJunchenTbOrders(JunchenMtOrderQueryCriteria criteria, Pageable pageable){
        return new ResponseEntity<>(mtOrderService.queryAll(criteria,pageable),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增mtOrder")
    @ApiOperation("新增mtOrder")
    @PreAuthorize("@el.check('admin','junchenMtOrder:add')")
    public ResponseEntity<Object> create(@Validated @RequestBody JunchenMtOrder resources){
        return new ResponseEntity<>(mtOrderService.save(resources),HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改mtOrder")
    @ApiOperation("修改mtOrder")
    @PreAuthorize("@el.check('admin','junchenMtOrder:edit')")
    public ResponseEntity<Object> update(@Validated @RequestBody JunchenMtOrder resources){
        mtOrderService.updateById(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除mtOrder")
    @ApiOperation("删除mtOrder")
    @PreAuthorize("@el.check('admin','junchenMtOrder:del')")
    @DeleteMapping
    public ResponseEntity<Object> deleteAll(@RequestBody Long[] ids) {
        Arrays.asList(ids).forEach(id->{
            mtOrderService.removeById(id);
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
