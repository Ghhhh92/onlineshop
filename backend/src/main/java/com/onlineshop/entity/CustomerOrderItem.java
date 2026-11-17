package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细，对应 customer_order_item 表。商品名与单价是下单时的快照。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Data
@TableName("customer_order_item")
public class CustomerOrderItem {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 id */
    private Long orderId;

    /** 商品 id */
    private Long productId;

    /** 下单时的商品名称 */
    private String productName;

    /** 下单时的单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;
}
