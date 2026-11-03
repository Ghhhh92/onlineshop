package com.onlineshop.controller;

import com.onlineshop.auth.RequireLogin;
import com.onlineshop.common.Result;
import com.onlineshop.dto.CategoryRequest;
import com.onlineshop.entity.Category;
import com.onlineshop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家维护商品类别（US-32），全部需要登录。
 *
 * @author 高佳豪
 * @date 2026-11-03
 */
@RequireLogin
@RestController
@RequestMapping("/api/seller/category")
@RequiredArgsConstructor
public class SellerCategoryController {

    private final CategoryService categoryService;

    /**
     * 新增类别：parentId 为空或 0 时是一级类别，传一级类别 id 时是该类别下的二级类别
     */
    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.create(request));
    }

    /**
     * 改名或调整顺序
     */
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.update(id, request));
    }

    /**
     * 删除类别：有子类别或下面还有商品时拒绝
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }
}
