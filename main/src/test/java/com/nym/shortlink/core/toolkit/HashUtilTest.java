package com.nym.shortlink.core.toolkit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    @DisplayName("相同输入产生相同输出")
    void sameInputProducesSameOutput() {
        String input = "https://example.com/test";
        String result1 = HashUtil.hashToBase62(input);
        String result2 = HashUtil.hashToBase62(input);
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("不同输入产生不同输出")
    void differentInputsProduceDifferentOutputs() {
        String result1 = HashUtil.hashToBase62("https://example.com/a");
        String result2 = HashUtil.hashToBase62("https://example.com/b");
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("输出仅包含 Base62 字符集 [0-9a-zA-Z]")
    void outputContainsOnlyBase62Chars() {
        for (int i = 0; i < 100; i++) {
            String result = HashUtil.hashToBase62("test-" + i);
            assertTrue(result.matches("^[0-9a-zA-Z]+$"),
                    "Unexpected chars in: " + result);
        }
    }

    @Test
    @DisplayName("输出长度不超过 6 位")
    void outputLengthMax6() {
        for (int i = 0; i < 50; i++) {
            String result = HashUtil.hashToBase62("test-input-" + i);
            assertTrue(result.length() >= 1 && result.length() <= 6,
                    "Expected length 1-6 but got: " + result.length() + " for input test-input-" + i);
        }
    }

    @RepeatedTest(5)
    @DisplayName("同一输入多次调用幂等")
    void idempotentAcrossCalls() {
        String input = "https://github.com/magestacks";
        String expected = HashUtil.hashToBase62(input);
        for (int i = 0; i < 10; i++) {
            assertEquals(expected, HashUtil.hashToBase62(input));
        }
    }
}
