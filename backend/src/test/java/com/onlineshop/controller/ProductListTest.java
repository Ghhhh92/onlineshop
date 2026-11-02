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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品列表和详情：买家浏览（US-01、US-02、US-31）和卖家后台的商品列表（US-15）。
 * 每个用例先清空商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductListTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession seller;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("买家看到在售和交易中的商品，按发布时间倒序；已下架的看不到")
    void buyerListShowsOnSaleAndTrading() throws Exception {
        product("旧台灯", ProductStatus.ON_SALE, 1, null);
        product("交易中的书", ProductStatus.FROZEN, 2, null);
        product("已售出的椅子", ProductStatus.DELISTED, 3, null);
        product("新键盘", ProductStatus.ON_SALE, 4, null);

        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].name", contains("新键盘", "交易中的书", "旧台灯")))
                .andExpect(jsonPath("$.data.records[1].status").value("FROZEN"))
                .andExpect(jsonPath("$.data.records[1].statusText").value("已冻结"))
                .andExpect(jsonPath("$.data.records[0].stock").value(1));
    }

    @Test
    @DisplayName("一件商品都没有时返回空列表，买家端据此显示「暂无在售商品」")
    void buyerListEmpty() throws Exception {
        product("已售出的椅子", ProductStatus.DELISTED, 1, null);

        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records", empty()));
    }

    @Test
    @DisplayName("按类别筛选：只返回这个类别的商品")
    void buyerListByCategory() throws Exception {
        product("台灯", ProductStatus.ON_SALE, 1, 1L);
        product("小说", ProductStatus.ON_SALE, 2, 2L);
        product("没分类的杯子", ProductStatus.ON_SALE, 3, null);

        mockMvc.perform(get("/api/product/list").param("categoryId", "1"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("台灯"))
                .andExpect(jsonPath("$.data.records[0].categoryId").value(1));
    }

    @Test
    @DisplayName("分页：按页码和每页条数返回，total 是全部条数；页码小于 1 按第 1 页")
    void buyerListPaging() throws Exception {
        for (int i = 1; i <= 12; i++) {
            product("第" + i + "件", ProductStatus.ON_SALE, i, null);
        }

        mockMvc.perform(get("/api/product/list").param("page", "2").param("size", "5"))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.records[*].name", contains("第7件", "第6件", "第5件", "第4件", "第3件")));
        mockMvc.perform(get("/api/product/list").param("page", "3").param("size", "5"))
                .andExpect(jsonPath("$.data.records[*].name", contains("第2件", "第1件")));
        mockMvc.perform(get("/api/product/list").param("page", "0").param("size", "5"))
                .andExpect(jsonPath("$.data.records[0].name").value("第12件"));
    }

    @Test
    @DisplayName("买家看商品详情；交易中的照常能看，已下架或不存在返回 404")
    void buyerDetail() throws Exception {
        Product lamp = product("台灯", ProductStatus.ON_SALE, 1, 3L);
        Product book = product("交易中的书", ProductStatus.FROZEN, 2, null);
        Product chair = product("已售出的椅子", ProductStatus.DELISTED, 3, null);

        mockMvc.perform(get("/api/product/" + lamp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("台灯"))
                .andExpect(jsonPath("$.data.price").value(10.0))
                .andExpect(jsonPath("$.data.categoryId").value(3));
        mockMvc.perform(get("/api/product/" + book.getId()))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
        mockMvc.perform(get("/api/product/" + chair.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("商品不存在或已下架"));
        mockMvc.perform(get("/api/product/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("买家浏览不需要登录；卖家后台的列表和详情需要登录")
    void loginRequirement() throws Exception {
        mockMvc.perform(get("/api/product/list")).andExpect(status().isOk());
        mockMvc.perform(get("/api/seller/product/list")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/product/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("卖家后台列表：全部商品（含已下架）按发布时间倒序，可按状态筛选")
    void sellerList() throws Exception {
        product("台灯", ProductStatus.ON_SALE, 1, null);
        product("交易中的书", ProductStatus.FROZEN, 2, null);
        product("已售出的椅子", ProductStatus.DELISTED, 3, null);

        mockMvc.perform(get("/api/seller/product/list").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].name", contains("已售出的椅子", "交易中的书", "台灯")));
        mockMvc.perform(get("/api/seller/product/list").param("status", "DELISTED").session(seller))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].stock").value(0));
        mockMvc.perform(get("/api/seller/product/list").param("status", "SOLD").session(seller))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数格式不正确：status"));
    }

    @Test
    @DisplayName("卖家看商品详情：任何状态都能看；不存在返回 404")
    void sellerDetail() throws Exception {
        Product chair = product("已售出的椅子", ProductStatus.DELISTED, 1, null);

        mockMvc.perform(get("/api/seller/product/" + chair.getId()).session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("已售出的椅子"))
                .andExpect(jsonPath("$.data.statusText").value("已下架"));
        mockMvc.perform(get("/api/seller/product/999999").session(seller))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    /** 第 day 天发布的商品；已下架的库存为 0，其余为 1 */
    private Product product(String name, ProductStatus status, int day, Long categoryId) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(status == ProductStatus.DELISTED ? 0 : 1);
        product.setCategoryId(categoryId);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 10, day, 9, 0));
        productMapper.insert(product);
        return product;
    }
}
