package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.auth.SellerSession;
import com.onlineshop.common.Result;
import com.onlineshop.dto.ChangePasswordRequest;
import com.onlineshop.dto.LoginRequest;
import com.onlineshop.dto.SellerInfo;
import com.onlineshop.entity.Seller;
import com.onlineshop.service.SellerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家接口：登录、当前卖家、修改密码、退出
 *
 * @author 李帆
 * @date 2026-10-11
 */
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * 卖家登录（US-11）。成功后在 Session 里记下卖家 id，并更换 Session ID。
     */
    @PostMapping("/login")
    public Result<SellerInfo> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Seller seller = sellerService.login(request.getUsername(), request.getPassword());
        HttpSession session = httpRequest.getSession();
        // 登录后换一个新的 Session ID，防止登录前被别人塞进来的 Session ID 在登录后继续有效（会话固定攻击）
        httpRequest.changeSessionId();
        SellerSession.login(session, seller);
        return Result.success(SellerInfo.from(seller));
    }

    /**
     * 修改密码（US-12）。成功后当前登录失效，需要用新密码重新登录。
     */
    @RequireLogin
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpSession session) {
        sellerService.changePassword(SellerSession.currentSellerId(session),
                request.getOldPassword(), request.getNewPassword());
        session.invalidate();
        return Result.success();
    }

    /**
     * 当前登录的卖家。前端刷新页面后用它恢复登录状态，未登录返回 401。
     */
    @RequireLogin
    @GetMapping("/me")
    public Result<SellerInfo> me(HttpSession session) {
        Seller seller = sellerService.getById(SellerSession.currentSellerId(session));
        return Result.success(SellerInfo.from(seller));
    }

    /**
     * 退出登录：销毁 Session。没登录时调用也返回成功。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success();
    }
}
