/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.customer.service;
import com.junchen.common.service.BaseService;
import com.junchen.modules.customer.domain.MwStoreCustomer;
import com.junchen.modules.customer.service.dto.MwStoreCustomerDto;
import com.junchen.modules.customer.service.dto.MwStoreCustomerQueryCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.junchen.domain.PageResult;
/**
* @author Bug
* @date 2020-12-10
*/
public interface MwStoreCustomerService extends BaseService<MwStoreCustomer>{

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param pageable 分页参数
    * @return Map<String,Object>
    */
    PageResult<MwStoreCustomerDto>  queryAll(MwStoreCustomerQueryCriteria criteria, Pageable pageable);

    /**
    * 查询所有数据不分页
    * @param criteria 条件参数
    * @return List<MwStoreCustomerDto>
    */
    List<MwStoreCustomer> queryAll(MwStoreCustomerQueryCriteria criteria);

    /**
    * 导出数据
    * @param all 待导出的数据
    * @param response /
    * @throws IOException /
    */
    void download(List<MwStoreCustomerDto> all, HttpServletResponse response) throws IOException;
}
