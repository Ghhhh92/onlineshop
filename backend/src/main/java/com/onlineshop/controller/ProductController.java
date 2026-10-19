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
     * 查询买家首页展示的商品（US-01、US-02）：在售或交易中的那一件，都没有时 data 为 null。
     * 商品冻结时照常返回名称、描述、图片、价格，status 为 FROZEN，买家端据此显示「商品交易中」。
     */
    @GetMapping("/on-sale")
    public Result<ProductInfo> getOnSale() {
        Product product = productService.getCurrentProduct();
        return Result.success(product == null ? null : ProductInfo.from(product));
    }
}
