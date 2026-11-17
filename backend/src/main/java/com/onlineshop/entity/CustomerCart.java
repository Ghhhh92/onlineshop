package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车条目，对应 customer_cart 表。同一客户同一商品只有一条，数量累加。
 *
 * @author 范与恒
 * @date 2026-11-17
 */
@Data
@TableName("customer_cart")
public class CustomerCart {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户 id */
    private Long customerId;

    /** 商品 id */
    private Long productId;

    /** 数量，1～999 */
    private Integer quantity;

    /** 加入时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
