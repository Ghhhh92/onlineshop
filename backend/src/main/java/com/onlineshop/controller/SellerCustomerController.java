package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.CustomerInfo;
import com.onlineshop.dto.OrderInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.service.CustomerOrderService;
import com.onlineshop.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家后台的客户接口（US-38）：查看注册客户和客户的购买历史，全部需要卖家登录。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@RequireLogin
@RestController
@RequestMapping("/api/seller/customer")
@RequiredArgsConstructor
public class SellerCustomerController {

    private final CustomerService customerService;
    private final CustomerOrderService customerOrderService;

    /**
     * 注册客户列表（用户名、电话、默认交易地点、注册时间），按注册时间倒序分页；没有客户时 records 为空数组
     */
    @GetMapping("/list")
    public Result<PageResult<CustomerInfo>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.success(customerService.list(page, size));
    }

    /**
     * 某个客户的购买历史（商品、下单时间、交易结果），按下单时间倒序分页；客户不存在时返回 404
     */
    @GetMapping("/{id}/orders")
    public Result<PageResult<OrderInfo>> orders(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        customerService.checkExists(id);
        return Result.success(customerOrderService.orders(id, page, size));
    }
}
