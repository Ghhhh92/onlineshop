package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏条目，对应 customer_favorite 表。同一客户同一商品只有一条。
 *
 * @author 范与恒
 * @date 2026-11-17
 */
@Data
@TableName("customer_favorite")
public class CustomerFavorite {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户 id */
    private Long customerId;

    /** 商品 id */
    private Long productId;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
