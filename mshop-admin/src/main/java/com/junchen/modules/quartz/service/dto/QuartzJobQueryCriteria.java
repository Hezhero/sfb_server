/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By syh.hzhero.cn
 */
package com.junchen.modules.quartz.service.dto;

import com.junchen.annotation.Query;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
* @author huangyu
* @date 2020-05-13
*/
@Data
public class QuartzJobQueryCriteria{

    @Query(type = Query.Type.INNER_LIKE)
    private String jobName;

    @Query
    private Boolean isSuccess;

    @Query
    private Boolean isPause;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
