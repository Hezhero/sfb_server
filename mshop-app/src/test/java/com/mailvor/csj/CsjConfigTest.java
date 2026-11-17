package com.mailvor.csj;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mailvor.modules.csj.config.CsjConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 穿山甲激励视频配置契约测试（工单 09）。
 *
 * <p>接缝：{@link CsjConfig#isReady()} 与 {@link CsjConfig#logStatus()} 的公开行为。
 * 测试直接 {@code new CsjConfig()} 并用 setter 注入，不依赖 Spring 上下文与真实数据库。</p>
 */
public class CsjConfigTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger configLogger;

    @Before
    public void setUp() {
        appender = new ListAppender<ILoggingEvent>();
        appender.start();
        configLogger = (Logger) LoggerFactory.getLogger(CsjConfig.class);
        configLogger.addAppender(appender);
    }

    @After
    public void tearDown() {
        configLogger.detachAppender(appender);
        appender.stop();
    }

    private static CsjConfig config(boolean enabled, String appId, String videoId) {
        CsjConfig config = new CsjConfig();
        config.setEnabled(enabled);
        config.setAppId(appId);
        config.setRewardedVideoId(videoId);
        return config;
    }

    private String loggedMessages() {
        StringBuilder sb = new StringBuilder();
        List<ILoggingEvent> events = appender.list;
        for (ILoggingEvent event : events) {
            sb.append(event.getFormattedMessage()).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void isReadyRequiresSwitchOnAndBothMandatoryFields() {
        assertTrue("开关开 + appId + 代码位齐全才可用",
                config(true, "5000001", "102038").isReady());

        assertFalse("开关关闭时即使配置齐全也不可用",
                config(false, "5000001", "102038").isReady());
        assertFalse("appId 为空时不可用",
                config(true, "", "102038").isReady());
        assertFalse("appId 为空白时不可用",
                config(true, "   ", "102038").isReady());
        assertFalse("appId 为 null 时不可用",
                config(true, null, "102038").isReady());
        assertFalse("代码位为空时不可用",
                config(true, "5000001", "").isReady());
        assertFalse("代码位为空白时不可用",
                config(true, "5000001", "   ").isReady());
        assertFalse("代码位为 null 时不可用",
                config(true, "5000001", null).isReady());
    }

    @Test
    public void logStatusMasksAppIdWithoutPrintingPlaintext() {
        // 首尾各保留 4 位，中间不出现在日志中
        String secretAppId = "500000112345678";
        CsjConfig config = config(true, secretAppId, "102038");

        config.logStatus();

        String logs = loggedMessages();
        assertFalse("appId 明文不得出现在日志中", logs.contains(secretAppId));
        assertTrue("脱敏值应保留头部", logs.contains("5000"));
        assertTrue("脱敏值应保留尾部", logs.contains("5678"));
        assertTrue("脱敏串应使用星号遮挡中间段", logs.contains("****"));
    }

    @Test
    public void logStatusHandlesDisabledAndMissingAppId() {
        // 未启用：只打一条说明，不打印任何 appId
        CsjConfig disabled = config(false, "500000112345678", "102038");
        disabled.logStatus();
        assertTrue(loggedMessages().contains("未启用"));
        assertFalse(loggedMessages().contains("500000112345678"));

        // 启用但 appId 缺失：应提示未配置并给出警告，且绝不打印空串以外的信息
        appender.list.clear();
        CsjConfig missing = config(true, "", "102038");
        missing.logStatus();
        String logs = loggedMessages();
        assertTrue(logs.contains("未配置"));
        assertTrue(logs.contains("配置不完整"));
    }

    @Test
    public void maskAppIdReturnsSafeValueForShortAndEmptyInput() {
        assertEquals("未配置", CsjConfig.maskAppId(""));
        assertEquals("未配置", CsjConfig.maskAppId("   "));
        assertEquals("未配置", CsjConfig.maskAppId(null));
        assertEquals("长度异常(4)", CsjConfig.maskAppId("abcd"));
        assertEquals("长度异常(8)", CsjConfig.maskAppId("12345678"));
        assertEquals("1234****abcd", CsjConfig.maskAppId("12345678abcd"));
    }
}
