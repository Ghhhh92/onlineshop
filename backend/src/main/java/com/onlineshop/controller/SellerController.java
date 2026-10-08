package com.onlineshop.controller;

import com.onlineshop.common.Result;
import com.onlineshop.dto.LoginRequest;
import com.onlineshop.entity.Seller;
import com.onlineshop.service.SellerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 卖家接口
 *
 * @author 李帆
 * @date 2026-10-08
 */
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * 卖家登录
     */
    @PostMapping("/login")
    public Result<Seller> login(@Valid @RequestBody LoginRequest req, HttpSession session) {
        Seller seller = sellerService.login(req.getUsername(), req.getPassword());
        session.setAttribute("sellerId", seller.getId());
        // 不返回密码
        seller.setPassword(null);
        return Result.success(seller);
    }

    /**
     * 获取当前登录卖家信息
     */
    @GetMapping("/me")
    public Result<Seller> me(HttpSession session) {
        Object sellerId = session.getAttribute("sellerId");
        if (sellerId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(null);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }
}
