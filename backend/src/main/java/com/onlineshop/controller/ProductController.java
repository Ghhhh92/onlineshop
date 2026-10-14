package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.dto.PublishProductRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口。商品发布后不提供修改接口（US-28），只能下架后重新发布。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 发布商品（卖家后台，需要登录）
     */
    @RequireLogin
    @PostMapping("/publish")
    public Result<ProductInfo> publish(@Valid @RequestBody PublishProductRequest request) {
        return Result.success(ProductInfo.from(productService.publish(request)));
    }

    /**
     * 查询当前在售商品（买家端首页），没有在售商品时 data 为 null
     */
    @GetMapping("/on-sale")
    public Result<ProductInfo> getOnSale() {
        Product product = productService.getOnSaleProduct();
        return Result.success(product == null ? null : ProductInfo.from(product));
    }
}
