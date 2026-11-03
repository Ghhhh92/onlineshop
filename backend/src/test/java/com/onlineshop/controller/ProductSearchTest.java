package com.onlineshop.controller;

import com.onlineshop.entity.Product;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 关键词搜索（US-34）：按商品名称或描述匹配，可与类别筛选叠加；没有结果时返回空列表。
 * 每个用例先清空商品，结束后回滚。
 *
 * @author 叶文锦
 * @date 2026-11-03
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("按商品名称搜索：只返回名称匹配的商品")
    void searchByName() throws Exception {
        product("台灯", "九成新", null, ProductStatus.ON_SALE, 1);
        product("小说", "畅销书", null, ProductStatus.ON_SALE, 2);
        product("键盘", "机械键盘", null, ProductStatus.ON_SALE, 3);

        mockMvc.perform(get("/api/product/list").param("keyword", "台灯"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("台灯"));
    }

    @Test
    @DisplayName("按描述搜索：只返回描述匹配的商品")
    void searchByDescription() throws Exception {
        product("台灯", "九成新，送灯泡", null, ProductStatus.ON_SALE, 1);
        product("键盘", "全新未拆封", null, ProductStatus.ON_SALE, 2);

        mockMvc.perform(get("/api/product/list").param("keyword", "灯泡"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("台灯"));
    }

    @Test
    @DisplayName("关键词与类别筛选叠加：返回交集")
    void searchCombinedWithCategory() throws Exception {
        product("台灯", "九成新", 1L, ProductStatus.ON_SALE, 1);
        product("台灯", "九成新", 2L, ProductStatus.ON_SALE, 2);
        product("台灯", "九成新", null, ProductStatus.ON_SALE, 3);

        mockMvc.perform(get("/api/product/list").param("keyword", "台灯").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].categoryId").value(1));
    }

    @Test
    @DisplayName("搜索只返回在售和交易中的商品，已下架的不出现")
    void searchSkipsDelisted() throws Exception {
        product("台灯", "九成新", null, ProductStatus.ON_SALE, 1);
        product("台灯", "九成新", null, ProductStatus.DELISTED, 2);

        mockMvc.perform(get("/api/product/list").param("keyword", "台灯"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }

    @Test
    @DisplayName("关键词首尾空格会去掉")
    void keywordTrimmed() throws Exception {
        product("台灯", "九成新", null, ProductStatus.ON_SALE, 1);

        mockMvc.perform(get("/api/product/list").param("keyword", "  台灯  "))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("没有匹配结果：返回空列表，买家端据此显示「没有找到商品」")
    void noMatch() throws Exception {
        product("台灯", "九成新", null, ProductStatus.ON_SALE, 1);

        mockMvc.perform(get("/api/product/list").param("keyword", "不存在"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records", empty()));
    }

    private Product product(String name, String description, Long categoryId, ProductStatus status, int day) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(status == ProductStatus.DELISTED ? 0 : 1);
        product.setCategoryId(categoryId);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 11, day, 9, 0));
        productMapper.insert(product);
        return product;
    }
}
