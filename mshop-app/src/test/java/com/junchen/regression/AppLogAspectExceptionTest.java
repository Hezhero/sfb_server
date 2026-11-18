package com.junchen.regression;

import com.junchen.common.aspect.AppLogAspect;
import com.junchen.common.bean.LocalUser;
import com.junchen.dozer.service.IGenerator;
import com.junchen.modules.logging.aop.log.AppLog;
import com.junchen.modules.logging.domain.Log;
import com.junchen.modules.logging.service.LogService;
import com.junchen.modules.logging.service.impl.LogServiceImpl;
import com.junchen.modules.logging.service.mapper.LogMapper;
import com.junchen.utils.ThrowableUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 工单 15 回归测试：@AppLog 方法抛业务异常时，日志切面不得用自身 NPE 淹没原始异常。
 */
public class AppLogAspectExceptionTest {

    /** 模拟被 @AppLog 标注、且抛业务异常的服务方法（只有 @AppLog，没有 @Log）。 */
    public static class AppAnnotatedService {
        @AppLog(value = "下单", type = 1)
        public void placeOrder(String orderNo) {
            throw new IllegalStateException("订单创建失败:库存不足");
        }

        public void unannotated() {
            // 供 saveApp 空注解降级测试使用
        }
    }

    private final LogService logService = mock(LogService.class);

    @Before
    public void setUpRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LocalUser.clear();
    }

    private AppLogAspect aspect() {
        return new AppLogAspect(logService);
    }

    /** 切面在 @AfterThrowing 中依赖 @Around 预置的 currentTime，单测需手动预置。 */
    private void armCurrentTime(AppLogAspect aspect) {
        @SuppressWarnings("unchecked")
        ThreadLocal<Long> currentTime = (ThreadLocal<Long>) ReflectionTestUtils.getField(aspect, "currentTime");
        currentTime.set(System.currentTimeMillis());
    }

    @Test
    public void throwingAdviceRoutesToSaveAppAndKeepsOriginalExceptionDetail() throws Exception {
        Method method = AppAnnotatedService.class.getMethod("placeOrder", String.class);
        ProceedingJoinPoint joinPoint = joinPointFor(method, "SO-1001");
        IllegalStateException original = new IllegalStateException("订单创建失败:库存不足");
        AppLogAspect aspect = aspect();
        armCurrentTime(aspect);

        aspect.logAfterThrowing(joinPoint, original);

        // @AppLog 异常必须走 saveApp，绝不能走 save（save 读取 @Log 注解返回 null → NPE）
        verify(logService, never()).save(anyString(), anyString(), any(ProceedingJoinPoint.class),
                any(Log.class), anyLong());
        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(logService).saveApp(anyString(), anyString(), eq(joinPoint), logCaptor.capture(), anyLong());
        Log saved = logCaptor.getValue();
        assertEquals("ERROR", saved.getLogType());
        assertNotNull(saved.getExceptionDetail());
        String detail = new String(saved.getExceptionDetail());
        assertEquals(ThrowableUtil.getStackTrace(original), detail);
        assertTrue(detail.contains("IllegalStateException"));
        assertTrue(detail.contains("订单创建失败:库存不足"));
    }

    @Test
    public void appLogMethodBusinessExceptionPropagatesUnchangedThroughRealAspectChain() throws Exception {
        // 复现真实缺陷：旧实现在 @AfterThrowing 调用 save()，其内部 method.getAnnotation(Log.class) 为 null → NPE，
        // 该 NPE 会替换业务异常向外抛出。这里让 mock 的 save() 模拟这个 NPE。
        LogService buggySave = mock(LogService.class);
        doThrow(new NullPointerException("模拟旧实现 save() 的 NPE"))
                .when(buggySave).save(anyString(), anyString(), any(ProceedingJoinPoint.class),
                        any(Log.class), anyLong());
        AppLogAspect aspect = new AppLogAspect(buggySave);

        AspectJProxyFactory factory = new AspectJProxyFactory(new AppAnnotatedService());
        factory.addAspect(aspect);
        AppAnnotatedService proxy = factory.getProxy();

        try {
            proxy.placeOrder("SO-1001");
            fail("预期业务异常应当向外传播");
        } catch (IllegalStateException thrown) {
            // 必须是原始业务异常，不能被切面自身 NPE 替换
            assertEquals("订单创建失败:库存不足", thrown.getMessage());
        }
        verify(buggySave, never()).save(anyString(), anyString(), any(ProceedingJoinPoint.class),
                any(Log.class), anyLong());
        verify(buggySave).saveApp(anyString(), anyString(), any(ProceedingJoinPoint.class),
                any(Log.class), anyLong());
    }

    @Test
    public void saveAppDegradesWithoutNpeWhenAppLogAnnotationIsMissing() throws Exception {
        LogMapper mapper = mock(LogMapper.class);
        LogServiceImpl service = new LogServiceImpl(mapper, mock(IGenerator.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        Method unannotated = AppAnnotatedService.class.getMethod("unannotated");
        ProceedingJoinPoint joinPoint = joinPointFor(unannotated);
        Log log = new Log("ERROR", 1L);

        // 缺陷现状：method.getAnnotation(AppLog.class) == null 后仍访问 aopLog.value() → NPE
        // 传入含空格的 IP 使地理信息查询快速失败（getCityInfo 内部吞异常），避免测试依赖外网
        service.saveApp("tester", " ", joinPoint, log, 1L);

        // 注解缺失时安全降级为默认值，绝不抛 NPE
        assertEquals("", log.getDescription());
        assertEquals(Integer.valueOf(0), log.getType());
        assertEquals("ERROR", log.getLogType());
    }

    private ProceedingJoinPoint joinPointFor(Method method, Object... args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn((Signature) signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(method.getName());
        when(signature.getParameterNames()).thenReturn(parameterNames(method.getParameterCount()));
        when(joinPoint.getTarget()).thenReturn(new AppAnnotatedService());
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private static String[] parameterNames(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "arg" + i;
        }
        return names;
    }
}
