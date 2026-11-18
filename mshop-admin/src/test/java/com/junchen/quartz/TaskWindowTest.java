package com.junchen.quartz;

import com.junchen.modules.quartz.utils.TaskParams;
import com.junchen.modules.quartz.utils.TaskWindow;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 采集任务时间窗契约测试。
 *
 * <p><b>为什么需要</b>：多个实时采集任务反复写「解析参数 → 若未给时间窗则取现在往前
 * N 分钟」；历史回溯任务写「若未给则取最近 N 天」。这段逻辑散在各任务里，
 * 每个新平台都要抄一遍，容易抄错（单位、默认值、边界）。
 * TaskWindow 把「窗口计算」收成一个 seam。</p>
 */
public class TaskWindowTest {

    @Test
    public void minutesWindowDefaultsWhenParamsEmpty() {
        long before = System.currentTimeMillis() / 1000;
        TaskWindow.MinutesWindow w = TaskWindow.minutes(null, 10, "测试任务");

        assertTrue("起点应早于终点", w.start() < w.end());
        assertEquals("默认 10 分钟窗口", 600L, w.end() - w.start());
        assertTrue(w.end() >= before - 2);
    }

    @Test
    public void minutesWindowHonorsExplicitParam() {
        TaskWindow.MinutesWindow w = TaskWindow.minutes(
                TaskParams.parseObject("{\"minutes\":30}", "测试任务"), 10, "测试任务");
        assertEquals(1800L, w.end() - w.start());
    }

    @Test
    public void minutesWindowClampsNonPositiveToDefault() {
        TaskWindow.MinutesWindow w = TaskWindow.minutes(
                TaskParams.parseObject("{\"minutes\":0}", "测试任务"), 10, "测试任务");
        assertEquals(600L, w.end() - w.start());
    }

    @Test
    public void unixSecondsWindowUsesProvidedValues() {
        TaskWindow.UnixWindow w = TaskWindow.unixSeconds(1700000000L, 1700000600L);
        assertEquals(1700000000L, w.start());
        assertEquals(1700000600L, w.end());
    }

    @Test
    public void unixSecondsWindowDefaultsToRecentMinutes() {
        long before = System.currentTimeMillis() / 1000;
        TaskWindow.UnixWindow w = TaskWindow.unixSeconds(null, null, 10);
        assertEquals(600L, w.end() - w.start());
        assertTrue(w.end() >= before - 2);
    }

    @Test
    public void unixSecondsWindowSwapsReversedRange() {
        TaskWindow.UnixWindow w = TaskWindow.unixSeconds(200L, 100L);
        assertTrue("起止颠倒时应交换", w.start() < w.end());
    }
}
