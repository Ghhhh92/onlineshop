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
 * 订单全流程（挑战需求 B）：下单、商家确认/备货/发货/交易完成、客户与商家的取消边界。
 * 每个用例结束后回滚。
 *
 * @author 高佳豪
 * @date 2026-11-17
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession customer;
    private MockHttpSession seller;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM customer_order_item");
        jdbcTemplate.update("DELETE FROM customer_order");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM customer");

        customer = (MockHttpSession) mockMvc.perform(post("/api/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"abc12345\",\"phone\":\"13800000001\","
                                + "\"defaultLocation\":\"食堂\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("下单：订单进入客户下单，金额按单价乘数量，商品名与单价作为快照保存，库存不扣")
    void placeOrder() throws Exception {
        Product product = product("台灯", "35.00", 5);

        mockMvc.perform(post("/api/customer/order")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.statusText").value("客户下单"))
                .andExpect(jsonPath("$.data.totalAmount").value(70.0))
                .andExpect(jsonPath("$.data.items[0].productName").value("台灯"))
                .andExpect(jsonPath("$.data.items[0].price").value(35.0))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty());

        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("下单校验：已下架商品、库存不足都整单拦截")
    void placeRejectsInvalidProduct() throws Exception {
        Product delisted = product("旧台灯", "20.00", 5);
        delisted.setStatus(ProductStatus.DELISTED);
        productMapper.updateById(delisted);
        Product few = product("小说", "12.50", 1);

        mockMvc.perform(post("/api/customer/order")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + delisted.getId() + ",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品「旧台灯」已下架，不能下单"));

        mockMvc.perform(post("/api/customer/order")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + few.getId() + ",\"quantity\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品「小说」库存不足"));
    }

    @Test
    @DisplayName("完整推进：确认、备货完成、开始发货都不扣库存，交易完成时扣减，库存归零转为售罄")
    void fullLifecycle() throws Exception {
        Product product = product("台灯", "35.00", 5);
        long orderId = place(product, 2);

        mockMvc.perform(post("/api/seller/order/" + orderId + "/confirm").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        mockMvc.perform(post("/api/seller/order/" + orderId + "/prepare").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARED"));
        mockMvc.perform(post("/api/seller/order/" + orderId + "/ship").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
        assertThat(productMapper.selectById(product.getId()).getStock()).isEqualTo(5);

        mockMvc.perform(post("/api/seller/order/" + orderId + "/complete").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        Product after = productMapper.selectById(product.getId());
        assertThat(after.getStock()).isEqualTo(3);
        assertThat(after.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("交易完成扣到 0：商品自动转为售罄")
    void completeToZeroBecomesSalesOut() throws Exception {
        Product product = product("孤品", "99.00", 2);
        long orderId = place(product, 2);
        advanceToShipped(orderId);

        mockMvc.perform(post("/api/seller/order/" + orderId + "/complete").session(seller))
                .andExpect(status().isOk());

        Product after = productMapper.selectById(product.getId());
        assertThat(after.getStock()).isZero();
        assertThat(after.getStatus()).isEqualTo(ProductStatus.SALES_OUT);
    }

    @Test
    @DisplayName("开始发货校验库存：已发货量加本单数量超过库存时拒绝发货，订单停在备货完成")
    void shipRejectsWhenStockNotEnough() throws Exception {
        Product product = product("台灯", "35.00", 2);
        long first = place(product, 2);
        advanceToShipped(first);

        // 第二笔订单同样买 2 件：已发货 2 件占住了库存，再发就超了
        long second = place(product, 2);
        mockMvc.perform(post("/api/seller/order/" + second + "/confirm").session(seller));
        mockMvc.perform(post("/api/seller/order/" + second + "/prepare").session(seller));

        mockMvc.perform(post("/api/seller/order/" + second + "/ship").session(seller))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品「台灯」库存不足，无法发货"));

        mockMvc.perform(get("/api/seller/order/" + second).session(seller))
                .andExpect(jsonPath("$.data.status").value("PREPARED"));
    }

    @Test
    @DisplayName("客户取消：开始发货之前可以取消，已发货之后不能取消")
    void customerCancelBoundary() throws Exception {
        Product product = product("台灯", "35.00", 5);

        long pending = place(product, 1);
        mockMvc.perform(post("/api/customer/order/" + pending + "/cancel").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        long shipped = place(product, 1);
        advanceToShipped(shipped);
        mockMvc.perform(post("/api/customer/order/" + shipped + "/cancel").session(customer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单已发货，不能取消"));
    }

    @Test
    @DisplayName("商家取消：已发货也能取消，交易完成后不能取消")
    void sellerCancelBoundary() throws Exception {
        Product product = product("台灯", "35.00", 5);

        long shipped = place(product, 1);
        advanceToShipped(shipped);
        mockMvc.perform(post("/api/seller/order/" + shipped + "/cancel").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        long completed = place(product, 1);
        advanceToShipped(completed);
        mockMvc.perform(post("/api/seller/order/" + completed + "/complete").session(seller));
        mockMvc.perform(post("/api/seller/order/" + completed + "/cancel").session(seller))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单已交易完成，不能取消"));
    }

    @Test
    @DisplayName("客户只能取消和查看自己的订单，别人的订单按不存在处理")
    void cannotTouchOthersOrder() throws Exception {
        Product product = product("台灯", "35.00", 5);
        long orderId = place(product, 1);

        MockHttpSession other = (MockHttpSession) mockMvc.perform(post("/api/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lisi\",\"password\":\"abc12345\",\"phone\":\"13800000002\","
                                + "\"defaultLocation\":\"食堂\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post("/api/customer/order/" + orderId + "/cancel").session(other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customer/order/" + orderId).session(other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customer/order/list").session(other))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("未登录：客户下单、卖家推进订单都返回 401")
    void requireLogin() throws Exception {
        mockMvc.perform(post("/api/customer/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/seller/order/1/confirm"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 辅助方法 =====

    private long place(Product product, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/customer/order")
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
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
