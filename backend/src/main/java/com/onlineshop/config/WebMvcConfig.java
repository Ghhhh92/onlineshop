package com.onlineshop.config;

import com.onlineshop.auth.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册登录拦截器
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器只对标了 @RequireLogin 的接口生效，其余接口直接放行
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
    }
}
