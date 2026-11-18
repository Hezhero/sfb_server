package com.junchen.modules.user.vo;

import com.junchen.modules.user.service.dto.MUserBillDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * @ClassName BillVo
 * @author huangyu
 * @Date 2019/11/12
 **/
@Data
public class BillVo {
    private String time;
    @JsonIgnore
    private String ids;
    private List<MUserBillDto> list;
}
