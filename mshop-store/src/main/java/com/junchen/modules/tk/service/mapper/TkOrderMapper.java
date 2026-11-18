/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.tk.service.mapper;

import com.junchen.common.mapper.CoreMapper;
import com.junchen.modules.tk.domain.TkOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
* @author shenji
* @date 2022-08-29
*/
@Repository
public interface TkOrderMapper extends CoreMapper<TkOrder> {
}
