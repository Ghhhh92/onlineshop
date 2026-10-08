package com.onlineshop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布商品请求
 *
 * @author 李帆
 * @date 2026-10-08
 */
@Data
public class PublishProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    private String image;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;
}
