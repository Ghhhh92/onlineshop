package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.dto.PublishProductRequest;
import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductImageMapper;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.statemachine.ProductStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
    private final ProductImageMapper productImageMapper;
    private final ProductStateMachine productStateMachine;

    /**
     * 发布商品（US-13、US-31）：商品以「在售」状态保存。可以同时有多件商品在售，已有在售或交易中的商品时照样能发布。
     * <p>
     * 名称去掉首尾空格，没填的描述和图片存为 null；价格统一保留两位小数；没选类别的归入未分类；
     * 发布时间精确到秒，和数据库 DATETIME 存下来的一致，发布时返回的和之后查到的相同。
     */
    @Transactional
    public Product publish(PublishProductRequest request) {
        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(blankToNull(request.getDescription()));
        product.setImage(blankToNull(request.getImage()));
        // 入参校验已保证最多两位小数，这里只补齐成两位，不会发生舍入
        product.setPrice(request.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        productMapper.insert(product);
        if (product.getImage() != null) {
            productImageMapper.insertAll(product.getId(), List.of(product.getImage()));
        }
        return product;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 只展示一件商品的买家首页用：最新发布的在售或已冻结（交易中）商品，没有时返回 null
     */
    public Product getCurrentProduct() {
        return productMapper.selectCurrent();
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
