package com.onlineshop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineshop.common.BizException;
import com.onlineshop.common.Pages;
import com.onlineshop.dto.PageResult;
import com.onlineshop.dto.ProductInfo;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 买家浏览商品（US-01、US-02、US-31）：不需要登录。
 * 交易中（已冻结）的商品照常展示，status 为 FROZEN，买家端据此显示「商品交易中」；已下架的买家看不到。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Service
@RequiredArgsConstructor
public class BuyerProductService {

    private final ProductMapper productMapper;

    /**
     * 在售和交易中的商品，按发布时间倒序分页；categoryId 不为空时只看这个类别的商品
     */
    public PageResult<ProductInfo> list(Long categoryId, int page, int size) {
        Page<Product> result = productMapper.selectBuyerPage(Pages.of(page, size), categoryId);
        return new PageResult<>(result.getRecords().stream().map(ProductInfo::from).toList(), result.getTotal());
    }

    /**
     * 商品详情：不存在或已下架时返回 404
     */
    public ProductInfo detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == ProductStatus.DELISTED) {
            throw new BizException(404, "商品不存在或已下架");
        }
        return ProductInfo.from(product);
    }
}
