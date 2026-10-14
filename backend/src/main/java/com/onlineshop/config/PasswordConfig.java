package com.onlineshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt：每次加密自动加随机盐，同一个密码两次加密结果不同；只能校验，不能解密。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
