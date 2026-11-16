package com.mailvor.modules.tk.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 大淘客 scroll_id 分页器（工单 05）。
 *
 * <p><b>为什么需要它</b>：{@code get-newest-goods}（api-d.html?id=3）、
 * {@code get-stale-goods-by-time}（id=11）、{@code pull-goods-by-time}（id=12）
 * 三个接口用同一套分页协议，官方原文：
 * 「首次传入 1，后续传入接口返回的 pageid，接口将持续返回符合条件的完整商品列表」。
 * 游标由上游签发，<b>不能</b>本地按页码 +1 推算——页码方式在临近页会取到重复商品，
 * 而「商品入库」场景必须避免重复入口，所以只能用 scroll_id。
 * 三处各写一遍翻页循环必然写歪，故抽成这唯一的实现。</p>
 *
 * <p><b>终止条件</b>（任一满足即停，缺一不可）：</p>
 * <ol>
 *   <li>返回的列表为空 —— 官方约定的「没有更多数据」</li>
 *   <li>返回的 pageId 为空 —— 上游没给下一页游标</li>
 *   <li>返回的 pageId 与本次请求的游标相同 —— 再请求只会拿到同一页，会死循环</li>
 *   <li>达到 {@code maxPages} —— 上游异常时的兜底，避免拖死神庙线程</li>
 * </ol>
 *
 * <p>上游序列化异常（{@code page} 或 {@code list} 为 null）同样按「没有更多数据」处理，
 * 而不是当成分页继续——否则一次脏报文就会变成死循环。</p>
 */
@Slf4j
public final class DtkScrollPager {

    /** 官方约定的第一个游标 */
    public static final String FIRST_PAGE_ID = "1";

    /** 默认最多翻多少页，防止上游异常导致无限循环 */
    public static final int DEFAULT_MAX_PAGES = 200;

    private DtkScrollPager() {
    }

    /**
     * 一页上游数据。
     *
     * @param list   本页商品；null 与空表等价，都视为「没有更多」
     * @param pageId 上游签发的下一页游标；为空表示没有下一页
     */
    public static final class Page<T> {
        private final List<T> list;
        private final String pageId;

        public Page(List<T> list, String pageId) {
            this.list = list;
            this.pageId = pageId;
        }

        public static <T> Page<T> empty() {
            return new Page<>(Collections.<T>emptyList(), null);
        }

        public List<T> getList() {
            return list;
        }

        public String getPageId() {
            return pageId;
        }
    }

    /** 单页拉取回调。实现方只负责一次 HTTP 调用与解析，翻页逻辑交给本类。 */
    public interface PageFetcher<T> {
        Page<T> fetch(String pageId, int pageSize);
    }

    /**
     * 一次性把全部页拉完并合并。
     *
     * <p>只在数据量可控时使用；商品全量同步请用 {@link #forEachPage} 逐页处理，
     * 避免几十万条商品一次性堆在内存里。</p>
     */
    public static <T> List<T> collect(PageFetcher<T> fetcher, int pageSize, int maxPages) {
        final List<T> all = new ArrayList<>();
        forEachPage(fetcher, pageSize, maxPages, all::addAll);
        return all;
    }

    /** {@link #collect} 的默认上限版本 */
    public static <T> List<T> collect(PageFetcher<T> fetcher, int pageSize) {
        return collect(fetcher, pageSize, DEFAULT_MAX_PAGES);
    }

    /**
     * 逐页回调，调用方可边拉边落库。
     *
     * @return 实际拉取的页数（含最后那个空页）
     */
    public static <T> int forEachPage(PageFetcher<T> fetcher, int pageSize, int maxPages,
                                      Consumer<List<T>> consumer) {
        if (fetcher == null) {
            return 0;
        }
        int limit = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
        String cursor = FIRST_PAGE_ID;
        int pages = 0;

        for (int i = 0; i < limit; i++) {
            Page<T> page = fetcher.fetch(cursor, pageSize);
            pages++;

            // 上游返回 null / list 为 null：按「没有更多数据」处理，不能继续翻
            if (page == null || page.getList() == null || page.getList().isEmpty()) {
                break;
            }
            consumer.accept(page.getList());

            String next = page.getPageId();
            if (next == null || next.trim().isEmpty()) {
                break;                       // 上游没给下一页游标
            }
            if (next.trim().equals(cursor)) {
                // 游标没有前进：再请求只会拿到同一页
                log.warn("大淘客 scroll_id 分页游标未前进，停止翻页 cursor={}", cursor);
                break;
            }
            cursor = next.trim();
        }
        return pages;
    }
}
