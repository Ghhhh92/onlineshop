package com.onlineshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品图片，对应 product_image 表。一个商品可以有多张图片，按 sort 从小到大展示。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@Data
@TableName("product_image")
public class ProductImage {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 id */
    private Long productId;

    /** 图片地址 */
    private String url;

    /** 展示顺序，从 0 开始，0 是封面 */
    private Integer sort;
}
