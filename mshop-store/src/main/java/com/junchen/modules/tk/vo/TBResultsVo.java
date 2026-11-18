package com.junchen.modules.tk.vo;


import com.junchen.modules.tk.domain.JunchenTbOrder;
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
public class TBResultsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<JunchenTbOrder> publisher_order_dto;

}
