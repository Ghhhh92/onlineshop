package com.onlineshop.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 口令码生成（US-05、US-24）：8 位大写字母和数字。
 * <p>
 * 去掉了容易看错的 0、O、1、I，买家抄写、口头报给卖家时不容易出错。
 * 32 个字符取 8 位，约 1.1 万亿种组合；用 SecureRandom 生成，别人无法根据已有口令码推算出下一个。
 * 唯一性由调用方查重和数据库唯一约束共同保证。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@Component
public class IntentCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
