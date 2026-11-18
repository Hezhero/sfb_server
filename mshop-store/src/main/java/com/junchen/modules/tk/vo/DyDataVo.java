package com.junchen.modules.tk.vo;


import com.junchen.modules.tk.domain.JunchenDyOrder;
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
public class DyDataVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer total;

    private ArrayList<JunchenDyOrder> list;
}
