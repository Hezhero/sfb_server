package com.mailvor.modules.tk.service.mapper;

import com.mailvor.common.mapper.RootMapper;
import com.mailvor.modules.tk.domain.MwDtkGoods;
import org.springframework.stereotype.Repository;

/**
 * 大淘客商品库 Mapper（工单 05）。
 *
 * <p>继承 {@link RootMapper} 而不是 {@code CoreMapper}：商品同步是批量落库，
 * 需要 {@code mysqlInsertOrUpdateBath}（MySQL {@code ON DUPLICATE KEY UPDATE}）
 * 才能按 {@code dtk_id} 唯一键做真正的 upsert——价格变更时覆盖、新增时插入，
 * 一条语句搞定，避免「先查后写」的并发窗口。</p>
 */
@Repository
public interface MwDtkGoodsMapper extends RootMapper<MwDtkGoods> {
}
