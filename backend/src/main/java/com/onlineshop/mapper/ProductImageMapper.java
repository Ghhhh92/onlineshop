package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineshop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品图片数据访问。增删改查继承自 BaseMapper。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {

    /**
     * 按顺序保存一个商品的图片，第一张的 sort 为 0
     */
    default void insertAll(Long productId, List<String> urls) {
        for (int i = 0; i < urls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(urls.get(i));
            image.setSort(i);
            insert(image);
        }
    }

    /**
     * 一个商品的全部图片地址，按展示顺序排列；没有图片时为空列表
     */
    default List<String> selectUrls(Long productId) {
        return selectList(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSort))
                .stream().map(ProductImage::getUrl).toList();
    }

    /**
     * 一批商品的图片地址，一条 SQL 查出来：商品 id → 按展示顺序排列的图片地址；没有图片的商品不在结果里
     */
    default Map<Long, List<String>> selectUrlsByProducts(Collection<Long> productIds) {
        return selectList(Wrappers.<ProductImage>lambdaQuery()
                .in(ProductImage::getProductId, productIds)
                .orderByAsc(ProductImage::getProductId)
                .orderByAsc(ProductImage::getSort))
                .stream().collect(Collectors.groupingBy(ProductImage::getProductId,
                        Collectors.mapping(ProductImage::getUrl, Collectors.toList())));
    }
}
