package com.junchen.meituan;

import com.junchen.modules.meituan.MeituanService;
import com.junchen.modules.meituan.config.MeituanConfig;
import com.junchen.modules.meituan.param.MeituanOrderParam;
import com.junchen.modules.meituan.utils.MeituanUtil;
import com.junchen.modules.quartz.task.OrderTask;
import com.junchen.modules.tk.domain.JunchenMtOrder;
import com.junchen.modules.tk.param.QueryMtParam;
import com.junchen.modules.tk.service.JunchenMtOrderService;
import com.junchen.modules.tk.vo.MtDataVo;
import com.junchen.modules.tk.vo.MtResVo;
import com.junchen.utils.RedisUtil;
import com.junchen.utils.SpringContextUtils;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MeituanOrderSyncTest {
    private MtResVo response(JunchenMtOrder... records) {
        MtDataVo data = new MtDataVo();
        data.setPositionIndex("next-page");
        data.setRecords(new ArrayList<>(Arrays.asList(records)));
        MtResVo response = new MtResVo();
        response.setCode(200); response.setMsg(data);
        return response;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void officialSidPersistsWithoutUtmMediumAndForeignAppIsRejected() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        SetOperations<String, Object> sets = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(sets);
        ApplicationContext previous = SpringContextUtils.applicationContext;
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean("redisTemplate", RedisTemplate.class)).thenReturn(redis);
        SpringContextUtils.applicationContext = context;
        Object previousRedis = ReflectionTestUtils.getField(RedisUtil.class, "redisTemplate");
        ReflectionTestUtils.setField(RedisUtil.class, "redisTemplate", redis);
        try {
            OrderTask task = new OrderTask();
            JunchenMtOrderService database = mock(JunchenMtOrderService.class);
            ReflectionTestUtils.setField(task, "mtOrderService", database);
            JunchenMtOrder sfb = JunchenMtOrder.builder().orderId(123L).sid("sfbuid_42").build();
            JunchenMtOrder foreign = JunchenMtOrder.builder().orderId(124L).sid("tenant_1_uid_42").build();
            assertEquals("next-page", ReflectionTestUtils.invokeMethod(task, "saveMtOrder", response(sfb, foreign)));
            assertEquals(Long.valueOf(42), sfb.getUid());
            assertEquals(Long.valueOf(123), sfb.getUniqueItemId());
            verify(database).saveOrUpdateBatch(Collections.singletonList(sfb));
            verify(sets).add(anyString(), eq(42L));
        } finally {
            ReflectionTestUtils.setField(RedisUtil.class, "redisTemplate", previousRedis);
            SpringContextUtils.applicationContext = previous;
        }
    }

    @Test
    public void foreignOnlyPageKeepsCursorAndDoesNotPersist() {
        OrderTask task = new OrderTask();
        JunchenMtOrderService database = mock(JunchenMtOrderService.class);
        ReflectionTestUtils.setField(task, "mtOrderService", database);
        MeituanService provider = mock(MeituanService.class);
        ReflectionTestUtils.setField(task, "meituanService", provider);
        when(provider.order(any(MeituanOrderParam.class))).thenReturn(response());
        assertEquals("next-page", ReflectionTestUtils.invokeMethod(task, "saveMtCPS", new QueryMtParam()));
        JunchenMtOrder foreign = JunchenMtOrder.builder().sid("uid_42").build();
        assertEquals("next-page", ReflectionTestUtils.invokeMethod(task, "saveMtOrder", response(foreign)));
        verifyNoInteractions(database);
    }

    @Test
    public void legacyEncryptedMediumIsStillRecognizedAndInvalidMediumIsSafe() {
        OrderTask task = new OrderTask();
        MeituanConfig config = new MeituanConfig(); config.setAppKey("test-key");
        ReflectionTestUtils.setField(task, "meituanConfig", config);
        JunchenMtOrder legacy = JunchenMtOrder.builder()
                .utmMedium(MeituanUtil.encryptHex("sfbuid_42", "test-key")).build();
        assertEquals(Long.valueOf(42), ReflectionTestUtils.invokeMethod(task, "getMtOrderUid", legacy));
        legacy.setUtmMedium("bad-hex");
        assertNull(ReflectionTestUtils.invokeMethod(task, "getMtOrderUid", legacy));
        legacy.setSid("uid_42");
        legacy.setUtmMedium(MeituanUtil.encryptHex("sfbuid_42", "test-key"));
        assertNull(ReflectionTestUtils.invokeMethod(task, "getMtOrderUid", legacy));
    }
}
