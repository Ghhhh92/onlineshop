package com.onlineshop.controller;

import com.onlineshop.common.Result;
import com.onlineshop.dto.CategoryNode;
import com.onlineshop.entity.Category;
import com.onlineshop.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品类别接口（US-32）：两级类别树，买家和卖家都用它，不需要登录。
 *
 * @author 高佳豪
 * @date 2026-11-03
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryMapper categoryMapper;

    /**
     * 两级类别树：一级类别按 sort 升序，二级类别挂到所属一级下同样按 sort 升序。
     * 没有任何类别时返回空数组，前端显示「暂无类别」。
     */
    @GetMapping("/tree")
    public Result<List<CategoryNode>> tree() {
        List<Category> all = categoryMapper.selectList(null);
        all.sort(Comparator.comparing(Category::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Category::getId));

        Map<Long, CategoryNode> roots = new LinkedHashMap<>();
        List<CategoryNode> secondLevel = new ArrayList<>();
        for (Category category : all) {
            if (Category.ROOT_PARENT_ID == category.getParentId()) {
                roots.put(category.getId(), new CategoryNode(category.getId(), category.getName(), category.getSort()));
            } else {
                secondLevel.add(new CategoryNode(category.getId(), category.getName(), category.getSort()));
            }
        }
        // 二级挂到父节点下，找不到父节点的（数据异常）忽略，避免整棵树查不出来
        for (Category category : all) {
            if (Category.ROOT_PARENT_ID == category.getParentId()) {
                continue;
            }
            CategoryNode parent = roots.get(category.getParentId());
            if (parent != null) {
                parent.getChildren().add(new CategoryNode(category.getId(), category.getName(), category.getSort()));
            }
        }
        return Result.success(new ArrayList<>(roots.values()));
    }
}
