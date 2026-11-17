package com.onlineshop.controller;

import com.onlineshop.auth.CustomerSession;
import com.onlineshop.auth.RequireCustomerLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.CreateOrderRequest;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.OrderLine;
import com.onlineshop.dto.PageResult;
import com.onlineshop.enums.OrderStatus;
import com.onlineshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户订单接口（挑战需求 B）：直接下单、我的订单、订单详情、取消订单。都需要客户登录。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@RestController
@RequestMapping("/api/customer/order")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    /**
     * 直接下单（不经购物车），下单后订单进入「客户下单」
     */
    @RequireCustomerLogin
    @PostMapping
    public Result<OrderDetail> place(@Valid @RequestBody CreateOrderRequest request, HttpSession session) {
        Long customerId = CustomerSession.currentCustomerId(session);
        return Result.success(orderService.place(customerId,
                List.of(new OrderLine(request.getProductId(), request.getQuantity()))));
    }

    /**
     * 我的订单，按下单时间倒序分页，可按状态筛选
     */
    @RequireCustomerLogin
    @GetMapping("/list")
    public Result<PageResult<OrderDetail>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) OrderStatus status,
                                                HttpSession session) {
        return Result.success(orderService.customerOrders(
                CustomerSession.currentCustomerId(session), page, size, status));
    }

    /**
     * 订单详情，只能看自己的订单
     */
    @RequireCustomerLogin
    @GetMapping("/{id}")
    public Result<OrderDetail> detail(@PathVariable Long id, HttpSession session) {
        return Result.success(orderService.customerOrderDetail(
                CustomerSession.currentCustomerId(session), id));
    }

    /**
     * 客户取消订单，只能在「开始发货」之前
     */
    @RequireCustomerLogin
    @PostMapping("/{id}/cancel")
    public Result<OrderDetail> cancel(@PathVariable Long id, HttpSession session) {
        return Result.success(orderService.cancelByCustomer(
                CustomerSession.currentCustomerId(session), id));
    }
}
