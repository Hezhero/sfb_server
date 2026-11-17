package com.mailvor.modules.quartz.utils;

/**
 * 采集任务的时间窗计算：把「默认最近 N 分钟 / 最近 N 天」「Unix 秒换算」
 * 从各任务类里收进一处。
 *
 * <p>不抽取任务类本身——Quartz 用 {@code getDeclaredMethod} 反射调用
 * {@code run(String)}，方法必须声明在具体类上，因此每个任务仍保留自己的
 * 一行入口，只把窗口计算委托给这里。</p>
 */
public final class TaskWindow {

    private TaskWindow() {
    }

    /** JSON 参数式采集任务的窗口（起点/终点均为 Unix 秒） */
    public static final class MinutesWindow {
        private final long start;
        private final long end;
        public MinutesWindow(long start, long end) {
            this.start = start;
            this.end = end;
        }
        public long start() { return start; }
        public long end() { return end; }
    }

    /** 直接给 Unix 秒的窗口 */
    public static final class UnixWindow {
        private final long start;
        private final long end;
        public UnixWindow(long start, long end) {
            this.start = start;
            this.end = end;
        }
        public long start() { return start; }
        public long end() { return end; }
    }

    /**
     * 从 JSON 参数取 minutes（缺失/非正用默认值），算出最近 N 分钟的 Unix 秒窗口。
     */
    public static MinutesWindow minutes(com.alibaba.fastjson.JSONObject param,
                                        int defaultMinutes, String taskName) {
        Integer minutes = param == null ? null : param.getInteger("minutes");
        if (minutes == null || minutes <= 0) {
            minutes = defaultMinutes;
        }
        long end = System.currentTimeMillis() / 1000;
        return new MinutesWindow(end - minutes * 60L, end);
    }

    /**
     * 直接用 Unix 秒构造窗口；任一为空则回退到最近 defaultMinutes 分钟；
     * 起止颠倒时交换，保证 start < end。
     */
    public static UnixWindow unixSeconds(Long start, Long end, int defaultMinutes) {
        if (start == null || end == null) {
            long now = System.currentTimeMillis() / 1000;
            return new UnixWindow(now - defaultMinutes * 60L, now);
        }
        return start <= end ? new UnixWindow(start, end) : new UnixWindow(end, start);
    }

    /** 两参版本：都有值时直接用 */
    public static UnixWindow unixSeconds(long start, long end) {
        return start <= end ? new UnixWindow(start, end) : new UnixWindow(end, start);
    }
}
