package com.onlineshop.auth;

import com.onlineshop.common.BizException;
import com.onlineshop.mapper.CustomerMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 客户登录拦截：请求的接口标了 {@link RequireCustomerLogin} 时检查客户登录状态，不满足就返回 401。
 * <ul>
 *     <li>没有登录：「请先登录」；</li>
 *     <li>Session 里的客户已不存在：去掉客户登录，「登录已失效，请重新登录」。</li>
 * </ul>
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Component
@RequiredArgsConstructor
public class CustomerLoginInterceptor implements HandlerInterceptor {

    private final CustomerMapper customerMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !requiresCustomerLogin(method)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Long customerId = CustomerSession.currentCustomerId(session);
        if (customerId == null) {
            throw new BizException(401, "请先登录");
        }
        if (customerMapper.selectById(customerId) == null) {
            CustomerSession.logout(session);
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return true;
    }

    private static boolean requiresCustomerLogin(HandlerMethod method) {
        return method.hasMethodAnnotation(RequireCustomerLogin.class)
                || method.getBeanType().isAnnotationPresent(RequireCustomerLogin.class);
    }
}
