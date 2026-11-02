package com.onlineshop.service;

import com.onlineshop.dto.ProductInfo;
import com.onlineshop.entity.Product;
import com.onlineshop.mapper.ProductImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 把商品转成返回给前端的 {@link ProductInfo}，带上商品的全部图片。
 * 转一页商品时，这一页所有商品的图片用一条 SQL 查出来，不会每件商品查一次。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Component
@RequiredArgsConstructor
public class ProductInfoAssembler {

    private final ProductImageMapper productImageMapper;

    public ProductInfo toInfo(Product product) {
        return ProductInfo.from(product, productImageMapper.selectUrls(product.getId()));
    }

    public List<ProductInfo> toInfos(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> images = productImageMapper.selectUrlsByProducts(
                products.stream().map(Product::getId).toList());
        return products.stream()
                .map(product -> ProductInfo.from(product, images.getOrDefault(product.getId(), List.of())))
                .toList();
    }
}
