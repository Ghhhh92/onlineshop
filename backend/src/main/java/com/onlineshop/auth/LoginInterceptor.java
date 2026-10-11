package com.onlineshop.auth;

import com.onlineshop.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：请求的接口标了 {@link RequireLogin}，而当前没有登录的卖家时，返回 401「请先登录」。
 * <p>
 * 前端 axios 收到 401 会跳转到登录页。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method
                && requiresLogin(method)
                && SellerSession.currentSellerId(request.getSession(false)) == null) {
            throw new BizException(401, "请先登录");
        }
        return true;
    }

    private static boolean requiresLogin(HandlerMethod method) {
        return method.hasMethodAnnotation(RequireLogin.class)
                || method.getBeanType().isAnnotationPresent(RequireLogin.class);
    }
}
