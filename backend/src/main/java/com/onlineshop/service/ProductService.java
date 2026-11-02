package com.onlineshop.service;

import com.onlineshop.common.BizException;
import com.onlineshop.common.RichTextSanitizer;
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
    private final ProductImageService productImageService;
    private final ProductStateMachine productStateMachine;

    /**
     * 发布商品（US-13、US-31、US-35）：商品以「在售」状态保存。可以同时有多件商品在售，已有在售或交易中的商品时照样能发布。
     * <p>
     * 名称去掉首尾空格；描述按富文本过滤，没填时存为 null；图片只接受上传接口返回的地址，按顺序保存，第一张作为封面；
     * 价格统一保留两位小数；没选类别的归入未分类；
     * 发布时间精确到秒，和数据库 DATETIME 存下来的一致，发布时返回的和之后查到的相同。
     */
    @Transactional
    public Product publish(PublishProductRequest request) {
        List<String> images = checkImages(request.getImages());

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(RichTextSanitizer.sanitize(request.getDescription()));
        product.setImage(images.isEmpty() ? null : images.get(0));
        // 入参校验已保证最多两位小数，这里只补齐成两位，不会发生舍入
        product.setPrice(request.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        productMapper.insert(product);
        productImageMapper.insertAll(product.getId(), images);
        return product;
    }

    /**
     * 图片必须是上传接口保存过的（US-27）；重复的只留第一次出现的，顺序不变
     */
    private List<String> checkImages(List<String> urls) {
        if (urls == null) {
            return List.of();
        }
        List<String> images = urls.stream().distinct().toList();
        for (String url : images) {
            if (!productImageService.isStored(url)) {
                throw new BizException("图片地址无效，请重新上传");
            }
        }
        return images;
    }

    /**
     * 库存减 1（交易成功时调用，US-33），返回剩下的库存；剩 0 时调用方再把商品转为已下架。
     * 库存已经是 0 时提示库存不足。
     */
    @Transactional
    public int decreaseStock(Long id) {
        if (!productMapper.decreaseStock(id)) {
            getByIdOrThrow(id);
            throw new BizException("库存不足");
        }
        return productMapper.selectById(id).getStock();
    }

    /**
     * 卖家修改库存（补货，US-33）。商品因库存为 0 已下架时，经状态机恢复为在售，不用重新发布；
     * 在售、交易中的商品只改库存，状态不变。
     */
    @Transactional
    public Product updateStock(Long id, int stock) {
        Product product = productMapper.selectByIdForUpdate(id);
        if (product == null) {
            throw new BizException(404, "商品不存在");
        }
        if (product.getStatus() == ProductStatus.DELISTED) {
            productStateMachine.transit(product, ProductStatus.ON_SALE);
        }
        product.setStock(stock);
        productMapper.updateById(product);
        return product;
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
