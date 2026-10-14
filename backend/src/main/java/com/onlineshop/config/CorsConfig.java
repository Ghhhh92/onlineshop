package com.onlineshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：只允许 shop.cors.allowed-origins 里列出的前端地址带 Cookie 访问 /api/**。
 * <p>
 * 带 Cookie 的跨域请求不能对所有来源放行，否则任何网站都能以已登录卖家的身份调用后台接口。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final ShopProperties shopProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(shopProperties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
