package com.onlineshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品服务
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    /**
     * 单件在售约束（高佳豪负责的状态机部分：在售/冻结商品存在时拒绝发布新商品）
     */
    private void checkSingleOnSale() {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getStatus, ProductStatus.ON_SALE, ProductStatus.FROZEN));
        if (count != null && count > 0) {
            throw new BizException("已有在售或交易中的商品，无法发布新商品");
        }
    }

    /**
     * 发布商品
     */
    @Transactional
    public Product publish(String name, String description, String image, BigDecimal price) {
        // 单件在售约束
        checkSingleOnSale();

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setImage(image);
        product.setPrice(price);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    /**
     * 查询当前在售商品
     */
    public Product getOnSaleProduct() {
        return productMapper.selectOne(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, ProductStatus.ON_SALE));
    }
}
