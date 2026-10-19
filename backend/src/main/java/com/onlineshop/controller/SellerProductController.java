package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.IntentInfo;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.service.SellerProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 卖家后台的商品查询接口，全部需要登录。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@RequireLogin
@RestController
@RequestMapping("/api/seller/product")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductQueryService queryService;

    /**
     * 当前商品（在售或已冻结），没有时 data 为 null
     */
    @GetMapping("/current")
    public Result<ProductInfo> current() {
        return Result.success(queryService.currentProduct());
    }

    /**
     * 指定商品的意向购买人，按先到先得排列；没有意向时为空数组
     */
    @GetMapping("/{id}/intents")
    public Result<List<IntentInfo>> intents(@PathVariable Long id) {
        return Result.success(queryService.intents(id));
    }

    /**
     * 历史商品（已下架），按成交时间倒序分页
     */
    @GetMapping("/history")
    public Result<PageResult<ProductInfo>> history(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(queryService.history(page, size));
    }
}
