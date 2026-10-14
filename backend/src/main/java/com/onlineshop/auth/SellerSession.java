package com.onlineshop.auth;

import com.onlineshop.entity.Seller;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;

/**
 * 卖家登录态：登录成功后把卖家 id 和密码版本号存进 Session，浏览器靠 JSESSIONID Cookie 对应到这个 Session。
 *
 * @author 李帆
 * @date 2026-10-11
 */
public final class SellerSession {

    /** Session 里保存卖家 id 的属性名 */
    public static final String SELLER_ID = "sellerId";

    /** Session 里保存登录时密码版本号的属性名 */
    public static final String PASSWORD_VERSION = "passwordVersion";

    private SellerSession() {
    }

    /**
     * 记下登录的卖家
     */
    public static void login(HttpSession session, Seller seller) {
        session.setAttribute(SELLER_ID, seller.getId());
        session.setAttribute(PASSWORD_VERSION, seller.getPasswordVersion());
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

    /**
     * 登录之后卖家有没有改过密码：改过的话这个 Session 应当失效
     */
    public static boolean isPasswordChanged(HttpSession session, Seller seller) {
        return !Objects.equals(session.getAttribute(PASSWORD_VERSION), seller.getPasswordVersion());
    }
}
