package com.onlineshop.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类别树节点：一级类别带 children，二级类别 children 为空数组
 *
 * @author 高佳豪
 * @date 2026-11-03
 */
@Data
public class CategoryNode {

    private Long id;

    private String name;

    private Integer sort;

    private List<CategoryNode> children = new ArrayList<>();

    public CategoryNode(Long id, String name, Integer sort) {
        this.id = id;
        this.name = name;
        this.sort = sort;
    }
}
