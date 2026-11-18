package com.junchen.dataoke;

import com.junchen.modules.tk.util.DtkScrollPager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * scroll_id 分页器单元测试（工单 05）。
 *
 * <p><b>接缝</b>：{@link DtkScrollPager#collect} —— 一个纯函数式组件，
 * 不碰 HTTP、不碰数据库，因此可以逐条钉住官方描述的翻页语义。</p>
 *
 * <p><b>官方语义</b>（三个接口一致，见 api-d.html?id=3 / id=11 / id=12 的 pageId 说明）：
 * 「首次传入 1，后续传入接口返回的 pageid，接口将持续返回符合条件的完整商品列表」。
 * 也就是说游标由上游签发，本地不能自己 +1 推算页码。</p>
 */
public class DtkScrollPagerTest {

    /** 记录调用轨迹的假上游：按给定 sequence 依次返回页面，并记下被请求的游标。 */
    private static final class FakeUpstream<T> implements DtkScrollPager.PageFetcher<T> {
        private final List<DtkScrollPager.Page<T>> pages;
        final List<String> requestedPageIds = new ArrayList<>();
        final List<Integer> requestedPageSizes = new ArrayList<>();
        int calls = 0;

        @SafeVarargs
        FakeUpstream(DtkScrollPager.Page<T>... pages) {
            this.pages = Arrays.asList(pages);
        }

        @Override
        public DtkScrollPager.Page<T> fetch(String pageId, int pageSize) {
            requestedPageIds.add(pageId);
            requestedPageSizes.add(pageSize);
            if (calls >= pages.size()) {
                return DtkScrollPager.Page.empty();
            }
            return pages.get(calls++);
        }
    }

    @Test
    public void firstRequestUsesOneThenFollowsUpstreamCursorUntilEmptyPage() {
        FakeUpstream<String> upstream = new FakeUpstream<>(
                new DtkScrollPager.Page<>(Arrays.asList("a", "b"), "cursor-2"),
                new DtkScrollPager.Page<>(Collections.singletonList("c"), "cursor-3"),
                new DtkScrollPager.Page<>(Collections.emptyList(), "cursor-4"));

        List<String> collected = DtkScrollPager.collect(upstream, 100, 20);

        assertEquals(Arrays.asList("a", "b", "c"), collected);
        // 首次必须是 "1"，之后逐次使用上游返回的 pageId —— 不是页码自增
        assertEquals(Arrays.asList("1", "cursor-2", "cursor-3"), upstream.requestedPageIds);
        // pageSize 原样透传
        assertEquals(Arrays.asList(100, 100, 100), upstream.requestedPageSizes);
    }

    @Test
    public void emptyFirstPageTerminatesWithoutRequestingAgain() {
        FakeUpstream<String> upstream = new FakeUpstream<>(
                new DtkScrollPager.Page<>(Collections.emptyList(), "cursor-2"));

        assertTrue(DtkScrollPager.collect(upstream, 50, 20).isEmpty());
        assertEquals(Collections.singletonList("1"), upstream.requestedPageIds);
    }

    @Test
    public void nullPageOrNullListTerminatesInsteadOfSpinning() {
        DtkScrollPager.PageFetcher<String> nullPage = (pageId, pageSize) -> null;
        assertTrue(DtkScrollPager.collect(nullPage, 50, 20).isEmpty());

        FakeUpstream<String> upstream = new FakeUpstream<>(new DtkScrollPager.Page<>(null, "cursor-2"));
        assertTrue(DtkScrollPager.collect(upstream, 50, 20).isEmpty());
        assertEquals(1, upstream.requestedPageIds.size());
    }

    @Test
    public void missingOrRepeatedCursorStopsInsteadOfLoopingForever() {
        // 上游返回空游标：没有下一页了
        FakeUpstream<String> noCursor = new FakeUpstream<>(
                new DtkScrollPager.Page<>(Collections.singletonList("a"), null));
        assertEquals(Collections.singletonList("a"), DtkScrollPager.collect(noCursor, 10, 20));
        assertEquals(1, noCursor.requestedPageIds.size());

        // 上游把同一个游标原样返回：继续请求只会得到同一页，必须停
        FakeUpstream<String> stuck = new FakeUpstream<>(
                new DtkScrollPager.Page<>(Collections.singletonList("a"), "1"),
                new DtkScrollPager.Page<>(Collections.singletonList("a"), "1"));
        assertEquals(Collections.singletonList("a"), DtkScrollPager.collect(stuck, 10, 20));
        assertEquals(1, stuck.requestedPageIds.size());
    }

    @Test
    public void maxPagesCapsTheLoopSoOneBadUpstreamCannotRunForever() {
        // 上游永远返回新游标和数据：只能靠 maxPages 兜底
        DtkScrollPager.PageFetcher<String> endless = new DtkScrollPager.PageFetcher<String>() {
            int n = 0;

            @Override
            public DtkScrollPager.Page<String> fetch(String pageId, int pageSize) {
                return new DtkScrollPager.Page<>(Collections.singletonList("x"), "cursor-" + (++n));
            }
        };

        assertEquals(3, DtkScrollPager.collect(endless, 10, 3).size());
    }

    @Test
    public void batchConsumerSeesEachPageInsteadOfOneGiantList() {
        FakeUpstream<String> upstream = new FakeUpstream<>(
                new DtkScrollPager.Page<>(Arrays.asList("a", "b"), "cursor-2"),
                new DtkScrollPager.Page<>(Collections.singletonList("c"), "cursor-3"),
                new DtkScrollPager.Page<>(Collections.emptyList(), null));

        List<List<String>> batches = new ArrayList<>();
        int total = DtkScrollPager.forEachPage(upstream, 200, 20, batches::add);

        assertEquals(3, total);
        assertEquals(2, batches.size());
        assertEquals(Arrays.asList("a", "b"), batches.get(0));
        assertEquals(Collections.singletonList("c"), batches.get(1));
    }

    @Test
    public void nullFetcherIsAToleratedNoOp() {
        assertTrue(DtkScrollPager.collect(null, 10, 20).isEmpty());
        assertEquals(0, DtkScrollPager.forEachPage(null, 10, 20, batch -> {
            fail("不应回调");
        }));
    }
}
