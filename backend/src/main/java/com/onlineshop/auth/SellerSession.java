package com.onlineshop.auth;

import jakarta.servlet.http.HttpSession;

/**
 * 卖家登录态：登录成功后把卖家 id 存进 Session，浏览器靠 JSESSIONID Cookie 对应到这个 Session。
 *
 * @author 李帆
 * @date 2026-10-11
 */
public final class SellerSession {

    /** Session 里保存卖家 id 的属性名 */
    public static final String SELLER_ID = "sellerId";

    private SellerSession() {
    }

    /**
     * 当前登录的卖家 id，未登录（没有 Session 或 Session 里没有卖家）时返回 null
     */
    public static Long currentSellerId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return session.getAttribute(SELLER_ID) instanceof Long sellerId ? sellerId : null;
    }
}
