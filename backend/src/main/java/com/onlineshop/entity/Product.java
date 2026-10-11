package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.onlineshop.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品，对应 product 表。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Data
@TableName("product")
public class Product {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品描述，选填 */
    private String description;

    /** 商品图片地址，选填 */
    private String image;

    /** 价格，单位元，两位小数 */
    private BigDecimal price;

    /** 商品状态，在库里存枚举名（ON_SALE / FROZEN / DELISTED） */
    private ProductStatus status;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 成交时间，交易成功下架时记录 */
    private LocalDateTime finishTime;
}
