package com.onlineshop.controller;

import com.onlineshop.common.Result;
import com.onlineshop.dto.PublishProductRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品接口
 *
 * @author 李帆
 * @date 2026-10-08
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 发布商品（卖家后台）
     */
    @PostMapping("/publish")
    public Result<Product> publish(@Valid @RequestBody PublishProductRequest req) {
        Product product = productService.publish(
                req.getName(), req.getDescription(), req.getImage(), req.getPrice());
        return Result.success(product);
    }

    /**
     * 查询当前在售商品（买家端首页）
     */
    @GetMapping("/on-sale")
    public Result<Product> getOnSale() {
        return Result.success(productService.getOnSaleProduct());
    }
}
