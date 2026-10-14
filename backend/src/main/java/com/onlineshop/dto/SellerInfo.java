package com.onlineshop.dto;

import com.onlineshop.entity.Seller;

/**
 * 返回给前端的卖家信息，不含密码
 *
 * @param id       卖家 id
 * @param username 用户名
 * @author 李帆
 * @date 2026-10-11
 */
public record SellerInfo(Long id, String username) {

    public static SellerInfo from(Seller seller) {
        return new SellerInfo(seller.getId(), seller.getUsername());
    }
}
