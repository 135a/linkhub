package com.nym.shortlink.core.toolkit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomGeneratorTest {

    @Test
    @DisplayName("默认长度生成 6 位字符串")
    void defaultGenerateReturnsSixChars() {
        String result = RandomGenerator.generateRandom();
        assertEquals(6, result.length());
    }

    @Test
    @DisplayName("指定长度生成正确")
    void generateWithSpecifiedLength() {
        assertEquals(4, RandomGenerator.generateRandom(4).length());
        assertEquals(8, RandomGenerator.generateRandom(8).length());
        assertEquals(12, RandomGenerator.generateRandom(12).length());
    }

    @Test
    @DisplayName("字符集仅包含 [0-9a-zA-Z]")
    void outputContainsOnlyBase62Chars() {
        for (int i = 0; i < 100; i++) {
            String result = RandomGenerator.generateRandom(10);
            assertTrue(result.matches("^[0-9a-zA-Z]+$"),
                    "Unexpected chars in: " + result);
        }
    }

    @Test
    @DisplayName("连续生成高概率不重复")
    void highProbabilityNoDuplication() {
        Set<String> results = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            results.add(RandomGenerator.generateRandom(6));
        }
        assertTrue(results.size() > 1,
                "Should have more than 1 unique result, got: " + results.size());
    }

    @RepeatedTest(3)
    @DisplayName("零长度生成空字符串")
    void zeroLengthGeneratesEmptyString() {
        String result = RandomGenerator.generateRandom(0);
        assertEquals(0, result.length());
    }
}
