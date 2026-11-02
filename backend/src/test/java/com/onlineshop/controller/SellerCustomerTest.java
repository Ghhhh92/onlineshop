package com.onlineshop.controller;

import com.onlineshop.entity.Customer;
import com.onlineshop.entity.Product;
import com.onlineshop.entity.PurchaseIntent;
import com.onlineshop.enums.IntentStatus;
import com.onlineshop.enums.ProductStatus;
import com.onlineshop.mapper.CustomerMapper;
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 卖家查看注册客户及其购买历史（US-38）。每个用例先清空客户、商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerCustomerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerMapper customerMapper;

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
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM customer");
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("客户列表和购买历史都需要卖家登录，客户登录了也看不了")
    void requireSellerLogin() throws Exception {
        MockHttpSession customer = (MockHttpSession) mockMvc.perform(post("/api/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"abc12345\",\"phone\":\"13800000001\","
                                + "\"defaultLocation\":\"食堂\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/seller/customer/list")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/customer/1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/customer/list").session(customer)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("客户列表：用户名、电话、默认交易地点、注册时间，按注册时间倒序，不含密码")
    void customerList() throws Exception {
        customer("zhangsan", 1);
        customer("lisi", 2);
        customer("wangwu", 3);

        mockMvc.perform(get("/api/seller/customer/list").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].username", contains("wangwu", "lisi", "zhangsan")))
                .andExpect(jsonPath("$.data.records[0].phone").value("13800000003"))
                .andExpect(jsonPath("$.data.records[0].defaultLocation").value("图书馆门口"))
                .andExpect(jsonPath("$.data.records[0].createTime").value("2026-11-03 09:00:00"))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist());
        mockMvc.perform(get("/api/seller/customer/list").param("page", "2").param("size", "2").session(seller))
                .andExpect(jsonPath("$.data.records[*].username", contains("zhangsan")));
    }

    @Test
    @DisplayName("没有注册客户时返回空列表")
    void emptyCustomerList() throws Exception {
        mockMvc.perform(get("/api/seller/customer/list").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records", empty()));
    }

    @Test
    @DisplayName("购买历史：这个客户的下单记录按时间倒序，带商品和交易结果，成功、失败的都在")
    void orders() throws Exception {
        Customer zhang = customer("zhangsan", 1);
        Customer li = customer("lisi", 2);
        Product lamp = product("台灯", "35.00");
        Product book = product("小说", "12.50");
        intent(lamp, zhang, IntentStatus.SUCCESS, 1);
        intent(book, zhang, IntentStatus.FAILED, 2);
        intent(book, zhang, IntentStatus.QUEUING, 3);
        intent(lamp, li, IntentStatus.QUEUING, 4);
        intent(lamp, null, IntentStatus.QUEUING, 5);

        mockMvc.perform(get("/api/seller/customer/" + zhang.getId() + "/orders").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].productName", contains("小说", "小说", "台灯")))
                .andExpect(jsonPath("$.data.records[*].statusText", contains("排队中", "失败", "成功")))
                .andExpect(jsonPath("$.data.records[2].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records[2].productId").value(lamp.getId()))
                .andExpect(jsonPath("$.data.records[2].price").value(35.0))
                .andExpect(jsonPath("$.data.records[2].createTime").value("2026-11-01 10:00:00"));
        mockMvc.perform(get("/api/seller/customer/" + zhang.getId() + "/orders")
                        .param("page", "2").param("size", "2").session(seller))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }

    @Test
    @DisplayName("没下过单的客户返回空列表；客户不存在返回 404")
    void ordersEmptyOrMissing() throws Exception {
        Customer zhang = customer("zhangsan", 1);

        mockMvc.perform(get("/api/seller/customer/" + zhang.getId() + "/orders").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));
        mockMvc.perform(get("/api/seller/customer/999999/orders").session(seller))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("客户不存在"));
    }

    /** 第 day 天注册的客户，电话尾号是 day */
    private Customer customer(String username, int day) {
        Customer customer = new Customer();
        customer.setUsername(username);
        customer.setPassword("不会用来登录");
        customer.setPhone("1380000000" + day);
        customer.setDefaultLocation("图书馆门口");
        customer.setCreateTime(LocalDateTime.of(2026, 11, day, 9, 0));
        customerMapper.insert(customer);
        return customer;
    }

    private Product product(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.of(2026, 10, 30, 9, 0));
        productMapper.insert(product);
        return product;
    }

    /** 第 day 天 10 点提交的意向；customer 为 null 表示匿名提交 */
    private void intent(Product product, Customer customer, IntentStatus status, int day) {
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName("买家" + day);
        intent.setPhone("1390000000" + day);
        intent.setCode("ORDER00" + day);
        intent.setPosition(intentMapper.selectMaxPosition(product.getId()) + 1);
        intent.setStatus(status);
        intent.setCreateTime(LocalDateTime.of(2026, 11, day, 10, 0));
        intent.setCustomerId(customer == null ? null : customer.getId());
        intentMapper.insert(intent);
    }
}
