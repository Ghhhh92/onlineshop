package com.onlineshop.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 Controller 类或方法上，表示这个接口只有登录的卖家才能调用。
 * 未登录时由 {@link LoginInterceptor} 拦下，返回 401。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireLogin {
}
