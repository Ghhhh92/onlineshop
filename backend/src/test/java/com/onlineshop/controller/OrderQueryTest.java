package com.onlineshop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单查询接口（挑战需求 B 缺口补齐，叶文锦）：直接下单参数校验、订单号格式、客户/卖家订单列表的
 * 分页、倒序与状态筛选、订单详情快照、未登录校验。每个用例结束后回滚。
 *
 * @author 叶文锦
 * @date 2026-11-17
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession customer;
    private MockHttpSession other;
    private MockHttpSession seller;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM customer_order_item");
        jdbcTemplate.update("DELETE FROM customer_order");
        jdbcTemplate.update("DELETE FROM customer_cart");
        jdbcTemplate.update("DELETE FROM customer_favorite");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM customer");

        customer = register("zhangsan");
        other = register("lisi");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("直接下单参数校验：缺商品、缺数量、数量为 0、超过 999 都返回 400 具体文案")
    void placeValidation() throws Exception {
        Product product = product("台灯", "35.00", 5);

        mockMvc.perform(post("/api/customer/order").session(customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品不能为空"));
        mockMvc.perform(post("/api/customer/order").session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("数量不能为空"));
        mockMvc.perform(post("/api/customer/order").session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("数量至少为 1"));
        mockMvc.perform(post("/api/customer/order").session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("数量不能超过 999"));
    }

    @Test
    @DisplayName("订单号：下单时间到秒 + 4 位随机数字（18 位数字）")
    void orderNoFormat() throws Exception {
        Product product = product("台灯", "35.00", 5);

        JsonNode data = place(customer, product, 2);

        assertThat(data.path("orderNo").asText()).matches("\\d{18}");
    }

    @Test
    @DisplayName("我的订单：按下单时间倒序、只看本人，别的客户订单不出现")
    void customerListDescAndOnlyOwn() throws Exception {
        Product first = product("商品一", "10.00", 5);
        Product second = product("商品二", "20.00", 5);
        Product third = product("商品三", "30.00", 5);
        place(customer, first, 1);
        place(customer, second, 1);
        place(customer, third, 1); // 最新下单
        place(other, first, 1);    // 别人的订单

        mockMvc.perform(get("/api/customer/order/list").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(3))
                .andExpect(jsonPath("$.data.records[0].items[0].productName").value("商品三"))
                .andExpect(jsonPath("$.data.records[2].items[0].productName").value("商品一"));
    }

    @Test
    @DisplayName("我的订单：按状态筛选只返回对应状态的订单")
    void customerListFiltersByStatus() throws Exception {
        Product first = product("商品一", "10.00", 5);
        Product second = product("商品二", "20.00", 5);
        long canceledId = orderId(place(customer, first, 1));
        place(customer, second, 1);

        mockMvc.perform(post("/api/customer/order/" + canceledId + "/cancel").session(customer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/order/list").session(customer).param("status", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("CANCELED"))
                .andExpect(jsonPath("$.data.records[0].statusText").value("已取消"));
    }

    @Test
    @DisplayName("我的订单：分页按 page/size 返回对应页，total 为总数")
    void customerListPaginates() throws Exception {
        Product product = product("台灯", "10.00", 5);
        place(customer, product, 1);
        place(customer, product, 1);
        place(customer, product, 1);

        mockMvc.perform(get("/api/customer/order/list").session(customer)
                        .param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(2));
        mockMvc.perform(get("/api/customer/order/list").session(customer)
                        .param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    @DisplayName("客户订单详情：含商品名、单价、数量快照与金额")
    void customerDetailSnapshot() throws Exception {
        Product product = product("台灯", "35.50", 5);
        long id = orderId(place(customer, product, 3));

        mockMvc.perform(get("/api/customer/order/" + id).session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(106.5))
                .andExpect(jsonPath("$.data.items[0].productName").value("台灯"))
                .andExpect(jsonPath("$.data.items[0].price").value(35.5))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));
    }

    @Test
    @DisplayName("卖家订单列表：看到所有客户订单，可按状态筛选")
    void sellerListAllAndFilter() throws Exception {
        Product product = product("台灯", "35.00", 5);
        place(customer, product, 1);
        long otherOrder = orderId(place(other, product, 1));

        mockMvc.perform(get("/api/seller/order/list").session(seller))
                .andExpect(jsonPath("$.data.total").value(2));

        advanceToShipped(otherOrder);
        mockMvc.perform(get("/api/seller/order/list").session(seller).param("status", "SHIPPED"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("SHIPPED"));
    }

    @Test
    @DisplayName("卖家订单详情：含订单号与商品明细")
    void sellerDetail() throws Exception {
        Product product = product("台灯", "35.00", 5);
        long id = orderId(place(customer, product, 2));

        mockMvc.perform(get("/api/seller/order/" + id).session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].productName").value("台灯"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("未登录：客户订单列表/详情、卖家订单列表/详情都返回 401")
    void requireLogin() throws Exception {
        mockMvc.perform(get("/api/customer/order/list")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/order/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/order/list")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/order/1")).andExpect(status().isUnauthorized());
    }

    // ===== 辅助方法 =====

    private MockHttpSession register(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"abc12345\","
                                + "\"phone\":\"1380000000" + (username.length() % 10) + "\","
                                + "\"defaultLocation\":\"食堂\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private JsonNode place(MockHttpSession session, Product product, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customer/order").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private long orderId(JsonNode data) {
        return data.path("id").asLong();
    }

    private void advanceToShipped(long orderId) throws Exception {
        mockMvc.perform(post("/api/seller/order/" + orderId + "/confirm").session(seller))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/seller/order/" + orderId + "/prepare").session(seller))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/seller/order/" + orderId + "/ship").session(seller))
                .andExpect(status().isOk());
    }

    private Product product(String name, String price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.of(2026, 11, 1, 9, 0));
        productMapper.insert(product);
        return product;
    }
}
