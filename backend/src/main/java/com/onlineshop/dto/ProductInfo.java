package com.onlineshop.dto;

import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回给前端的商品信息
 *
 * @param id          商品 id
 * @param name        名称
 * @param description 描述，富文本（HTML，已过滤），没填时为 null
 * @param image       封面图片地址（第一张图片），没有图片时为 null
 * @param images      全部图片地址，按展示顺序排列，没有图片时为空数组
 * @param price       价格，固定两位小数
 * @param stock       库存数量
 * @param categoryId  所属二级类别 id，未分类时为 null
 * @param status      状态（ON_SALE / FROZEN / DELISTED）
 * @param statusText  状态的中文（在售 / 已冻结 / 已下架）
 * @param createTime  发布时间
 * @param finishTime  成交时间，未成交时为 null
 * @author 李帆
 * @date 2026-10-11
 */
public record ProductInfo(Long id, String name, String description, String image, List<String> images,
                          BigDecimal price, Integer stock, Long categoryId, ProductStatus status, String statusText,
                          LocalDateTime createTime, LocalDateTime finishTime) {

    /**
     * @param images 商品的全部图片地址，按展示顺序排列
     */
    public static ProductInfo from(Product product, List<String> images) {
        return new ProductInfo(product.getId(), product.getName(), product.getDescription(), product.getImage(),
                images, product.getPrice(), product.getStock(), product.getCategoryId(),
                product.getStatus(), product.getStatus().getDesc(),
                product.getCreateTime(), product.getFinishTime());
    }
}
