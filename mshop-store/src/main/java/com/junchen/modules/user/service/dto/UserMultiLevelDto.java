package com.junchen.modules.user.service.dto;


import com.junchen.modules.user.vo.MwSystemUserLevelQueryVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName UserLevelDto
 * @author huangyu
 * @Date 2019/12/6
 **/
@Data
public class UserMultiLevelDto implements Serializable {

    private List multiList;

}
