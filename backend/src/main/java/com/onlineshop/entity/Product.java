package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.onlineshop.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String image;

    private BigDecimal price;

    private ProductStatus status;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
