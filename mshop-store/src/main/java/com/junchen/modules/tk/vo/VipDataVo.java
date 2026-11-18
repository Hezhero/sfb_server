package com.junchen.modules.tk.vo;


import com.junchen.modules.tk.domain.JunchenVipOrder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * <p>
 * 商品表 查询结果对象
 * </p>
 *
 * @author shenji
 * @date 2019-10-19
 */
@Data
public class VipDataVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer page;
    private Integer pageSize;
    private Integer total;

    private ArrayList<JunchenVipOrder> orderInfoList;
}
