package com.junchen.modules.user.service.dto;


import com.junchen.modules.user.vo.MwSystemUserTaskQueryVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName TaskDto
 * @author huangyu
 * @Date 2019/12/6
 **/
@Data
public class TaskDto implements Serializable {
    private List<MwSystemUserTaskQueryVo> list;
    private Long reachCount;
    private List<MwSystemUserTaskQueryVo> task;
}
