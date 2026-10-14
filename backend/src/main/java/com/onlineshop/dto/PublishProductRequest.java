package com.onlineshop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布商品请求（US-13、US-26）：名称和价格必填，描述和图片选填。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Data
public class PublishProductRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不能超过 100 个字符")
    private String name;

    @Size(max = 2000, message = "商品描述不能超过 2000 个字符")
    private String description;

    /** 图片地址 */
    @Size(max = 500, message = "图片地址不能超过 500 个字符")
    private String image;

    /** 价格，单位元：大于 0，最多两位小数，不超过 99999999.99（数据库 DECIMAL(10,2) 的上限） */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "价格最多两位小数，且不能超过 99999999.99")
    private BigDecimal price;
}
