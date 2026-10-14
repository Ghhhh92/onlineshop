package com.onlineshop.dto;

import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 返回给前端的商品信息
 *
 * @param id          商品 id
 * @param name        名称
 * @param description 描述，没填时为 null
 * @param image       图片地址，没填时为 null
 * @param price       价格，固定两位小数
 * @param status      状态（ON_SALE / FROZEN / DELISTED）
 * @param statusText  状态的中文（在售 / 已冻结 / 已下架）
 * @param createTime  发布时间
 * @param finishTime  成交时间，未成交时为 null
 * @author 李帆
 * @date 2026-10-11
 */
public record ProductInfo(Long id, String name, String description, String image, BigDecimal price,
                          ProductStatus status, String statusText,
                          LocalDateTime createTime, LocalDateTime finishTime) {

    public static ProductInfo from(Product product) {
        return new ProductInfo(product.getId(), product.getName(), product.getDescription(), product.getImage(),
                product.getPrice(), product.getStatus(), product.getStatus().getDesc(),
                product.getCreateTime(), product.getFinishTime());
    }
}
