package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.onlineshop.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户订单，对应 customer_order 表。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@Data
@TableName("customer_order")
public class CustomerOrder {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，展示与沟通用 */
    private String orderNo;

    /** 下单客户 id */
    private Long customerId;

    /** 订单状态，在库里存枚举名 */
    private OrderStatus status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
