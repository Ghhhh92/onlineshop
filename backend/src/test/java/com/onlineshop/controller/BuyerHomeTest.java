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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 买家首页展示的商品：在售时正常展示（US-01），交易中时照常展示并带上状态（US-02）。
 * 每个用例先清空商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerHomeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("商品在售：返回商品信息，状态为在售")
    void onSale() throws Exception {
        product("台灯", ProductStatus.ON_SALE, null);

        mockMvc.perform(get("/api/product/on-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("台灯"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
    }

    @Test
    @DisplayName("商品交易中：名称、描述、图片、价格照常返回，状态为已冻结")
    void frozenStillShown() throws Exception {
        product("台灯", ProductStatus.FROZEN, null);

        mockMvc.perform(get("/api/product/on-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("台灯"))
                .andExpect(jsonPath("$.data.description").value("九成新"))
                .andExpect(jsonPath("$.data.image").value("https://example.com/lamp.jpg"))
                .andExpect(jsonPath("$.data.price").value(50.00))
                .andExpect(jsonPath("$.data.status").value("FROZEN"))
                .andExpect(jsonPath("$.data.statusText").value("已冻结"));
    }

    @Test
    @DisplayName("只有已下架的商品时 data 为 null")
    void onlyDelisted() throws Exception {
        product("卖掉的台灯", ProductStatus.DELISTED, LocalDateTime.of(2026, 10, 9, 10, 0));

        mockMvc.perform(get("/api/product/on-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private void product(String name, ProductStatus status, LocalDateTime finishTime) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("九成新");
        product.setImage("https://example.com/lamp.jpg");
        product.setPrice(new BigDecimal("50.00"));
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 10, 1, 9, 0));
        product.setFinishTime(finishTime);
        productMapper.insert(product);
    }
}
