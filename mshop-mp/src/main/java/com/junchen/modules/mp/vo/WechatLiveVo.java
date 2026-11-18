package com.junchen.modules.mp.vo;

import com.junchen.modules.mp.service.dto.MwWechatLiveDto;
import lombok.Data;

import java.util.List;

@Data
public class WechatLiveVo {

    private List<MwWechatLiveDto> content;

    private Long totalElements;

    private Integer pageNumber;

    private Integer lastPage;


}
