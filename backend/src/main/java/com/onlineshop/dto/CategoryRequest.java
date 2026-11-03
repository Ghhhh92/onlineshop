package com.onlineshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增 / 修改类别的请求
 *
 * @author 高佳豪
 * @date 2026-11-03
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "类别名称不能为空")
    private String name;

    /** 上级类别 id，一级类别传 0 或不传 */
    private Long parentId;

    /** 同级展示顺序，小的在前，不传按 0 处理 */
    private Integer sort;
}
