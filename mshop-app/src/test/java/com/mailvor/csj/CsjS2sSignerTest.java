package com.mailvor.csj;

import com.mailvor.modules.csj.service.CsjS2sSigner;
import org.junit.Test;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * 穿山甲 S2S 回调验签测试。
 *
 * <p>用标准 JDK MessageDigest 独立计算期望值，不复用被测类的 DigestUtil，
 * 避免"用同一个错误实现验证自己"。</p>
 */
public class CsjS2sSignerTest {

    private static String jdkSha256(String raw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Test
    public void signMatchesOfficialAlgorithm() throws Exception {
        String key = "7ca31ab0a59d69a42dd8abc7cf2d8fbd";
        String transId = "D5361u6643";
        String expected = jdkSha256(key + ":" + transId);
        assertEquals(expected, CsjS2sSigner.sign(key, transId));
    }

    @Test
    public void verifyAcceptsCorrectSign() throws Exception {
        String key = "testKey";
        String tx = "tx-12345";
        String sign = jdkSha256(key + ":" + tx);
        assertTrue(CsjS2sSigner.verify(key, tx, sign));
    }

    @Test
    public void verifyRejectsWrongSign() {
        assertFalse(CsjS2sSigner.verify("testKey", "tx-12345", "deadbeef"));
    }

    @Test
    public void verifyRejectsWrongTransId() throws Exception {
        String key = "testKey";
        String sign = jdkSha256(key + ":tx-A");
        assertFalse(CsjS2sSigner.verify(key, "tx-B", sign));
    }

    @Test
    public void verifyRejectsBlankInputs() {
        assertFalse(CsjS2sSigner.verify(null, "tx", "sign"));
        assertFalse(CsjS2sSigner.verify("key", null, "sign"));
        assertFalse(CsjS2sSigner.verify("key", "tx", null));
        assertFalse(CsjS2sSigner.verify("  ", "tx", "sign"));
        assertFalse(CsjS2sSigner.verify("key", "  ", "sign"));
        assertFalse(CsjS2sSigner.verify("key", "tx", "  "));
    }

    @Test
    public void verifyIsCaseSensitiveOnTransId() throws Exception {
        String key = "testKey";
        String sign = jdkSha256(key + ":TX-UPPER");
        assertTrue(CsjS2sSigner.verify(key, "TX-UPPER", sign));
        assertFalse(CsjS2sSigner.verify(key, "tx-upper", sign));
    }
}
