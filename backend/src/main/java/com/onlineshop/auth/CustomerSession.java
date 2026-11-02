package com.onlineshop.auth;

import com.onlineshop.entity.Customer;
import jakarta.servlet.http.HttpSession;

/**
 * 客户登录态：登录成功后把客户 id 存进 Session。和卖家登录态用不同的属性名，互不影响。
 *
 * @author 李帆
 * @date 2026-11-02
 */
public final class CustomerSession {

    /** Session 里保存客户 id 的属性名 */
    public static final String CUSTOMER_ID = "customerId";

    private CustomerSession() {
    }

    /**
     * 记下登录的客户
     */
    public static void login(HttpSession session, Customer customer) {
        session.setAttribute(CUSTOMER_ID, customer.getId());
    }

    /**
     * 退出客户登录：只去掉客户 id，同一浏览器里卖家的登录不受影响
     */
    public static void logout(HttpSession session) {
        session.removeAttribute(CUSTOMER_ID);
    }

    /**
     * 当前登录的客户 id，未登录（没有 Session 或 Session 里没有客户）时返回 null
     */
    public static Long currentCustomerId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return session.getAttribute(CUSTOMER_ID) instanceof Long customerId ? customerId : null;
    }
}
