package com.onlineshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineshop.common.BizException;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.statemachine.ProductStateMachine;
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
    private final ProductStateMachine productStateMachine;

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

    // ===== 以下状态流转方法由高佳豪（状态机负责人）提供，迭代 4 接入 Controller =====

    private Product getByIdOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return product;
    }

    /**
     * 冻结商品（队首意向进入线下交易）：在售 → 已冻结
     */
    @Transactional
    public Product freezeProduct(Long id) {
        Product product = getByIdOrThrow(id);
        productStateMachine.freeze(product);
        productMapper.updateById(product);
        return product;
    }

    /**
     * 解冻回退（交易失败 / 手动解冻）：已冻结 → 在售
     */
    @Transactional
    public Product unfreezeProduct(Long id) {
        Product product = getByIdOrThrow(id);
        productStateMachine.unfreeze(product);
        productMapper.updateById(product);
        return product;
    }

    /**
     * 下架商品（交易成功）：已冻结 → 已下架，记录成交时间
     */
    @Transactional
    public Product delistProduct(Long id) {
        Product product = getByIdOrThrow(id);
        productStateMachine.delist(product);
        product.setFinishTime(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }
}
