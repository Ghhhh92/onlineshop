package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.OrderDetail;
import com.onlineshop.dto.PageResult;
import com.onlineshop.enums.OrderStatus;
import com.onlineshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家订单接口（挑战需求 B）：订单列表、详情、确认、备货完成、发货、交易完成、取消。都需要卖家登录。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@RestController
@RequestMapping("/api/seller/order")
@RequireLogin
@RequiredArgsConstructor
public class SellerOrderController {

    private final OrderService orderService;

    /**
     * 订单列表，按下单时间倒序分页，可按状态筛选
     */
    @GetMapping("/list")
    public Result<PageResult<OrderDetail>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) OrderStatus status) {
        return Result.success(orderService.sellerOrders(page, size, status));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetail> detail(@PathVariable Long id) {
        return Result.success(orderService.sellerOrderDetail(id));
    }

    /**
     * 商家确认：客户下单 → 商家确认
     */
    @PostMapping("/{id}/confirm")
    public Result<OrderDetail> confirm(@PathVariable Long id) {
        return Result.success(orderService.confirm(id));
    }

    /**
     * 备货完成：商家确认 → 备货完成
     */
    @PostMapping("/{id}/prepare")
    public Result<OrderDetail> prepare(@PathVariable Long id) {
        return Result.success(orderService.prepare(id));
    }

    /**
     * 开始发货：备货完成 → 开始发货，校验库存但不扣减
     */
    @PostMapping("/{id}/ship")
    public Result<OrderDetail> ship(@PathVariable Long id) {
        return Result.success(orderService.ship(id));
    }

    /**
     * 交易完成：开始发货 → 交易完成，扣减库存
     */
    @PostMapping("/{id}/complete")
    public Result<OrderDetail> complete(@PathVariable Long id) {
        return Result.success(orderService.complete(id));
    }

    /**
     * 商家取消：交易完成之前都可以取消
     */
    @PostMapping("/{id}/cancel")
    public Result<OrderDetail> cancel(@PathVariable Long id) {
        return Result.success(orderService.cancelBySeller(id));
    }
}
