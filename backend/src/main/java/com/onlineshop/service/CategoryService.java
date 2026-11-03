package com.onlineshop.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.common.BizException;
import com.onlineshop.dto.CategoryRequest;
import com.onlineshop.entity.Category;
import com.onlineshop.entity.Product;
import com.onlineshop.mapper.CategoryMapper;
import com.onlineshop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 卖家维护商品类别（US-32）：最多两级，同级名称不重复；
 * 有下级类别或下面还挂着商品的类别不能删。
 *
 * @author 高佳豪
 * @date 2026-11-03
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Transactional
    public Category create(CategoryRequest request) {
        String name = request.getName().trim();
        long parentId = request.getParentId() == null ? Category.ROOT_PARENT_ID : request.getParentId();
        checkParent(parentId);
        checkDuplicate(parentId, name, null);

        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = getByIdOrThrow(id);
        String name = request.getName().trim();
        checkDuplicate(category.getParentId(), name, id);

        category.setName(name);
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        getByIdOrThrow(id);
        Long children = categoryMapper.selectCount(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException("该类别下还有子类别，请先删除子类别");
        }
        Long products = productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id));
        if (products != null && products > 0) {
            throw new BizException("该类别下还有商品，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    private Category getByIdOrThrow(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "类别不存在");
        }
        return category;
    }

    /**
     * 上级类别必须存在，且上级本身必须是一级类别（最多两级）
     */
    private void checkParent(long parentId) {
        if (parentId == Category.ROOT_PARENT_ID) {
            return;
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException("上级类别不存在");
        }
        if (parent.getParentId() != Category.ROOT_PARENT_ID) {
            throw new BizException("最多支持两级类别");
        }
    }

    /**
     * 同一个上级下类别名称不能重复
     */
    private void checkDuplicate(long parentId, String name, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, name)
                .ne(excludeId != null, Category::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("同一上级下已存在同名类别");
        }
    }
}
