package com.onlineshop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求（US-13、US-26、US-33、US-35）：名称、价格和库存必填，描述、图片和类别选填。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@Data
public class PublishProductRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不能超过 100 个字符")
    private String name;

    /** 富文本（HTML），可以嵌入图片和视频；保存前过滤掉脚本等不安全的内容 */
    @Size(max = 10000, message = "商品描述不能超过 10000 个字符")
    private String description;

    /** 图片地址，先用上传接口逐张上传；按展示顺序排列，第一张是封面 */
    @Size(max = 9, message = "最多 9 张图片")
    private List<String> images;

    /** 价格，单位元：大于 0，最多两位小数，不超过 99999999.99（数据库 DECIMAL(10,2) 的上限） */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "价格最多两位小数，且不能超过 99999999.99")
    private BigDecimal price;

    /** 库存数量，减到 0 时商品自动下架 */
    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为 1")
    @Max(value = 99999, message = "库存不能超过 99999")
    private Integer stock;

    /** 所属二级类别 id，不选时为未分类 */
    @Positive(message = "类别不正确")
    private Long categoryId;
}
