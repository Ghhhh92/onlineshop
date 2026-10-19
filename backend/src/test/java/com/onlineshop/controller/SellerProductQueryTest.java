package com.onlineshop.controller;

import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.ProductMapper;
import com.onlineshop.mapper.PurchaseIntentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 卖家后台查询：当前商品（US-15）、意向购买人（US-16）、历史商品（US-21）。
 * 每个用例先清空商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerProductQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PurchaseIntentMapper intentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession seller;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("三个查询接口都需要登录")
    void requireLogin() throws Exception {
        mockMvc.perform(get("/api/seller/product/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/product/1/intents")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/product/history")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("没有当前商品时 data 为 null")
    void currentNone() throws Exception {
        mockMvc.perform(get("/api/seller/product/current").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("商品冻结时，卖家看到的状态是已冻结")
    void currentFrozen() throws Exception {
        product("冻结中的台灯", ProductStatus.FROZEN, null);

        mockMvc.perform(get("/api/seller/product/current").session(seller))
                .andExpect(jsonPath("$.data.name").value("冻结中的台灯"))
                .andExpect(jsonPath("$.data.status").value("FROZEN"))
                .andExpect(jsonPath("$.data.statusText").value("已冻结"));
    }

    @Test
    @DisplayName("意向人按先到先得排列；仍在队列中的带位次，已结束的位次为空")
    void intentsInOrder() throws Exception {
        Product product = product("台灯", ProductStatus.FROZEN, null);
        intent(product, "甲", 1, IntentStatus.TRADING);
        intent(product, "乙", 2, IntentStatus.CANCELED);
        intent(product, "丙", 3, IntentStatus.QUEUING);
        intent(product, "丁", 4, IntentStatus.FAILED);

        mockMvc.perform(get("/api/seller/product/" + product.getId() + "/intents").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", contains("甲", "乙", "丙", "丁")))
                .andExpect(jsonPath("$.data[0].position").value(1))
                .andExpect(jsonPath("$.data[1].position").value(nullValue()))
                .andExpect(jsonPath("$.data[2].position").value(2))
                .andExpect(jsonPath("$.data[3].position").value(nullValue()))
                .andExpect(jsonPath("$.data[0].statusText").value("交易中"))
                .andExpect(jsonPath("$.data[0].phone").value("13800000001"));
    }

    @Test
    @DisplayName("没有意向时返回空数组；商品不存在返回 404")
    void intentsEmptyOrMissing() throws Exception {
        Product product = product("台灯", ProductStatus.ON_SALE, null);

        mockMvc.perform(get("/api/seller/product/" + product.getId() + "/intents").session(seller))
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/seller/product/999999/intents").session(seller))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @Test
    @DisplayName("历史商品只列已下架的，按成交时间倒序分页")
    void historyPaged() throws Exception {
        product("第一件", ProductStatus.DELISTED, LocalDateTime.of(2026, 10, 1, 10, 0));
        product("第二件", ProductStatus.DELISTED, LocalDateTime.of(2026, 10, 5, 10, 0));
        product("第三件", ProductStatus.DELISTED, LocalDateTime.of(2026, 10, 9, 10, 0));
        product("在售的", ProductStatus.ON_SALE, null);

        mockMvc.perform(get("/api/seller/product/history").param("page", "1").param("size", "2").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].name", contains("第三件", "第二件")))
                .andExpect(jsonPath("$.data.records[0].finishTime").value("2026-10-09 10:00:00"));
        mockMvc.perform(get("/api/seller/product/history").param("page", "2").param("size", "2").session(seller))
                .andExpect(jsonPath("$.data.records[*].name", contains("第一件")));
    }

    @Test
    @DisplayName("分页参数越界按边界处理，格式不对返回 400")
    void historyParams() throws Exception {
        product("第一件", ProductStatus.DELISTED, LocalDateTime.of(2026, 10, 1, 10, 0));

        mockMvc.perform(get("/api/seller/product/history").param("page", "0").param("size", "1000").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)));
        mockMvc.perform(get("/api/seller/product/history").param("page", "abc").session(seller))
                .andExpect(status().isBadRequest());
    }

    private Product product(String name, ProductStatus status, LocalDateTime finishTime) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("50.00"));
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 9, 30, 9, 0));
        product.setFinishTime(finishTime);
        productMapper.insert(product);
        return product;
    }

    private void intent(Product product, String name, int position, IntentStatus status) {
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName(name);
        intent.setPhone("1380000000" + position);
        intent.setCode("TESTCOD" + position);
        intent.setPosition(position);
        intent.setStatus(status);
        intent.setCreateTime(LocalDateTime.of(2026, 10, 19, 9, position));
        intentMapper.insert(intent);
    }
}
