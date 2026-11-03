package com.onlineshop.controller;

import com.onlineshop.auth.CustomerSession;
import com.onlineshop.auth.RequireCustomerLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.CustomerInfo;
import com.onlineshop.dto.LoginRequest;
import com.onlineshop.dto.OrderInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.RegisterRequest;
import com.onlineshop.entity.Customer;
import com.onlineshop.service.CustomerOrderService;
import com.onlineshop.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户接口：注册、登录、当前客户、退出。客户登录和卖家登录互相独立。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerOrderService customerOrderService;

    /**
     * 客户注册（US-36）。注册成功后直接处于登录状态。
     */
    @PostMapping("/register")
    public Result<CustomerInfo> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        Customer customer = customerService.register(request);
        login(httpRequest, customer);
        return Result.success(CustomerInfo.from(customer));
    }

    /**
     * 客户登录。成功后在 Session 里记下客户 id，并更换 Session ID。
     */
    @PostMapping("/login")
    public Result<CustomerInfo> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Customer customer = customerService.login(request.getUsername(), request.getPassword());
        login(httpRequest, customer);
        return Result.success(CustomerInfo.from(customer));
    }

    /**
     * 当前登录的客户。前端刷新页面后用它恢复登录状态，未登录返回 401。
     */
    @RequireCustomerLogin
    @GetMapping("/me")
    public Result<CustomerInfo> me(HttpSession session) {
        Customer customer = customerService.getById(CustomerSession.currentCustomerId(session));
        return Result.success(CustomerInfo.from(customer));
    }

    /**
     * 我的订单（US-37）：当前客户的历史下单记录，按时间倒序分页。未登录返回 401。
     */
    @RequireCustomerLogin
    @GetMapping("/orders")
    public Result<PageResult<OrderInfo>> orders(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                HttpSession session) {
        return Result.success(
                customerOrderService.orders(CustomerSession.currentCustomerId(session), page, size));
    }

    /**
     * 退出客户登录。没登录时调用也返回成功。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            CustomerSession.logout(session);
        }
        return Result.success();
    }

    private static void login(HttpServletRequest httpRequest, Customer customer) {
        HttpSession session = httpRequest.getSession();
        // 登录后换一个新的 Session ID，防止登录前被别人塞进来的 Session ID 在登录后继续有效（会话固定攻击）
        httpRequest.changeSessionId();
        CustomerSession.login(session, customer);
    }
}
