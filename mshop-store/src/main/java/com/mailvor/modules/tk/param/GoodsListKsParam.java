package com.mailvor.modules.tk.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 快手商品列表/搜索参数（ks_item_list）。
 * 官方规格见 .scratch/tri-platform-audit/issues/06-kuaishou.md。
 */
@Data
@ApiModel(value = "GoodsListKsParam", description = "快手商品列表/搜索参数")
public class GoodsListKsParam {
    @ApiModelProperty(value = "关键词")
    private String keyword;
    @ApiModelProperty(value = "分页游标，首页传 1")
    private Integer minId = 1;
    @ApiModelProperty(value = "每页条数")
    private Integer back = 20;
    /** 0默认 1佣金升 2佣金降 3价格升 4价格降 5销量升 6销量降 */
    @ApiModelProperty(value = "排序：0默认 1佣金升 2佣金降 3价格升 4价格降 5销量升 6销量降")
    private Integer sortType;
    private Double minPrice;
    private Double maxPrice;
    private Integer minSale;
    private Integer maxSale;
}
