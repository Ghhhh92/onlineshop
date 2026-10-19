package com.onlineshop.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 口令码格式：8 位大写字母和数字，不含容易看错的 0、O、1、I。
 *
 * @author 李帆
 * @date 2026-10-19
 */
class IntentCodeGeneratorTest {

    private final IntentCodeGenerator generator = new IntentCodeGenerator();

    @Test
    @DisplayName("生成 1000 个口令码：格式都对、互不重复")
    void formatAndUniqueness() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.next();
            assertThat(code).matches("^[A-HJ-NP-Z2-9]{8}$");
            codes.add(code);
        }
        assertThat(codes).hasSize(1000);
    }

    @Test
    @DisplayName("手机号打码：保留前三位和后四位")
    void maskPhone() {
        assertThat(BuyerIntentService.maskPhone("13800001234")).isEqualTo("138****1234");
    }
}
