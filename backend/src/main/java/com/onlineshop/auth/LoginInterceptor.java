package com.onlineshop.auth;

import com.onlineshop.common.BizException;
import com.onlineshop.entity.Seller;
import com.onlineshop.mapper.SellerMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：请求的接口标了 {@link RequireLogin} 时检查登录状态，不满足就返回 401。
 * <ul>
 *     <li>没有登录：「请先登录」；</li>
 *     <li>登录之后卖家改过密码（或账号已不存在）：销毁这个 Session，「登录已失效，请重新登录」。</li>
 * </ul>
 * 前端 axios 收到 401 会跳转到登录页。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final SellerMapper sellerMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !requiresLogin(method)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Long sellerId = SellerSession.currentSellerId(session);
        if (sellerId == null) {
            throw new BizException(401, "请先登录");
        }
        Seller seller = sellerMapper.selectById(sellerId);
        if (seller == null || SellerSession.isPasswordChanged(session, seller)) {
            session.invalidate();
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return true;
    }

    private static boolean requiresLogin(HandlerMethod method) {
        return method.hasMethodAnnotation(RequireLogin.class)
                || method.getBeanType().isAnnotationPresent(RequireLogin.class);
    }
}
