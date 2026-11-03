package com.onlineshop.controller;

import com.onlineshop.entity.Category;
import com.onlineshop.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品类别树（US-32）：买家筛选与卖家发布页共用的两级类别树，不需要登录。
 * 每个用例先清空类别，结束后回滚。
 *
 * @author 叶文锦
 * @date 2026-11-03
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM category");
    }

    @Test
    @DisplayName("没有任何类别时返回空数组")
    void emptyTree() throws Exception {
        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    @DisplayName("两级类别树：一级按 sort 升序，二级挂到所属一级下同样按 sort 升序")
    void twoLevelTree() throws Exception {
        Category digital = category("数码", 0L, 2);
        Category home = category("家居", 0L, 1);
        category("手机", digital.getId(), 1);
        category("笔记本", digital.getId(), 0);

        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("家居"))
                .andExpect(jsonPath("$.data[0].children", empty()))
                .andExpect(jsonPath("$.data[1].name").value("数码"))
                .andExpect(jsonPath("$.data[1].children", hasSize(2)))
                .andExpect(jsonPath("$.data[1].children[0].name").value("笔记本"))
                .andExpect(jsonPath("$.data[1].children[1].name").value("手机"));
    }

    @Test
    @DisplayName("二级类别找不到上级时忽略，不影响整棵树")
    void orphanSecondLevelIgnored() throws Exception {
        category("孤儿", 999999L, 0);

        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    @DisplayName("类别树不需要登录")
    void noLoginRequired() throws Exception {
        category("数码", 0L, 0);

        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    private Category category(String name, Long parentId, int sort) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(sort);
        category.setCreateTime(LocalDateTime.of(2026, 11, 3, 9, 0));
        categoryMapper.insert(category);
        return category;
    }
}
