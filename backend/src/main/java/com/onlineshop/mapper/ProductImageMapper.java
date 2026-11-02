package com.onlineshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlineshop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
}
