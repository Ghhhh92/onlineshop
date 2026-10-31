package com.onlineshop.config;

import com.onlineshop.auth.LoginInterceptor;
import com.onlineshop.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册登录拦截器，把上传的商品图片作为静态资源提供访问
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final ProductImageService productImageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器只对标了 @RequireLogin 的接口生效，其余接口直接放行
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /api/uploads/xxx.png 对应图片目录里的 xxx.png，买家浏览商品不需要登录
        String location = productImageService.directory().toUri().toString();
        registry.addResourceHandler(ProductImageService.URL_PREFIX + "**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
