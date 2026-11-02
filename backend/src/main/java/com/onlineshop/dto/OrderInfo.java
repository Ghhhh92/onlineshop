package com.onlineshop.dto;

import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户的一条下单记录：客户登录后提交的一条购买意向，带上商品信息和交易结果
 *
 * @param intentId    意向 id
 * @param productId   商品 id
 * @param productName 商品名称，商品已不存在时为 null
 * @param image       商品封面图片，没有图片时为 null
 * @param price       商品价格
 * @param createTime  下单（提交意向）时间
 * @param status      意向状态（QUEUING / TRADING / SUCCESS / FAILED / CANCELED）
 * @param statusText  状态的中文：排队中 / 交易中 / 成功 / 失败 / 已撤销
 * @author 李帆
 * @date 2026-11-02
 */
public record OrderInfo(Long intentId, Long productId, String productName, String image, BigDecimal price,
                        LocalDateTime createTime, IntentStatus status, String statusText) {

    /**
     * @param product 意向对应的商品，不存在时为 null
     */
    public static OrderInfo of(PurchaseIntent intent, Product product) {
        return new OrderInfo(intent.getId(), intent.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getImage(),
                product == null ? null : product.getPrice(),
                intent.getCreateTime(), intent.getStatus(), intent.getStatus().getDesc());
    }
}
