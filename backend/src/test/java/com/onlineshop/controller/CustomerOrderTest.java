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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 客户查看自己的历史下单（US-37）：登录后按时间倒序查看本人订单，成功、失败的都保留；未登录 401。
 * 每个用例先清空客户、商品和意向，结束后回滚。
 *
 * @author 叶文锦
 * @date 2026-11-03
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerOrderTest {

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

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product_image");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM customer");
    }

    @Test
    @DisplayName("我的订单按时间倒序，带商品名和交易结果，成功、失败的都在")
    void ordersDescWithStatus() throws Exception {
        MockHttpSession session = registeredSession("zhangsan");
        Long zhang = customerMapper.selectByUsername("zhangsan").getId();
        Product lamp = product("台灯", "35.00");
        Product book = product("小说", "12.50");
        intent(lamp, zhang, IntentStatus.SUCCESS, 1);
        intent(book, zhang, IntentStatus.FAILED, 2);
        intent(book, zhang, IntentStatus.QUEUING, 3);

        mockMvc.perform(get("/api/customer/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[*].productName", contains("小说", "小说", "台灯")))
                .andExpect(jsonPath("$.data.records[*].statusText", contains("排队中", "失败", "成功")))
                .andExpect(jsonPath("$.data.records[2].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records[2].price").value(35.0))
                .andExpect(jsonPath("$.data.records[2].createTime").value("2026-11-01 10:00:00"));
    }

    @Test
    @DisplayName("只看得到本人的订单，其他客户的不出现")
    void onlyOwnOrders() throws Exception {
        MockHttpSession session = registeredSession("zhangsan");
        Long zhang = customerMapper.selectByUsername("zhangsan").getId();
        Long lisi = customer("lisi").getId();
        Product lamp = product("台灯", "35.00");
        intent(lamp, zhang, IntentStatus.SUCCESS, 1);
        intent(lamp, lisi, IntentStatus.SUCCESS, 2);

        mockMvc.perform(get("/api/customer/orders").session(session))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].productName").value("台灯"));
    }

    @Test
    @DisplayName("没下过单时返回空列表")
    void emptyOrders() throws Exception {
        MockHttpSession session = registeredSession("zhangsan");

        mockMvc.perform(get("/api/customer/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records", empty()));
    }

    @Test
    @DisplayName("未登录查看我的订单：返回 401")
    void requireLogin() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    private MockHttpSession registeredSession(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"abc12345\",\"phone\":\"13800000001\","
                                + "\"defaultLocation\":\"食堂\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    /** 第 day 天注册的客户 */
    private Customer customer(String username) {
        Customer customer = new Customer();
        customer.setUsername(username);
        customer.setPassword("不会用来登录");
        customer.setPhone("13800000001");
        customer.setDefaultLocation("食堂");
        customer.setCreateTime(LocalDateTime.of(2026, 11, 1, 9, 0));
        customerMapper.insert(customer);
        return customer;
    }

    private Product product(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(1);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCreateTime(LocalDateTime.of(2026, 10, 30, 9, 0));
        productMapper.insert(product);
        return product;
    }

    /** 第 day 天 10 点提交的意向 */
    private void intent(Product product, Long customerId, IntentStatus status, int day) {
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName("买家" + day);
        intent.setPhone("1390000000" + day);
        intent.setCode("ORDER00" + day);
        intent.setPosition(intentMapper.selectMaxPosition(product.getId()) + 1);
        intent.setStatus(status);
        intent.setCreateTime(LocalDateTime.of(2026, 11, day, 10, 0));
        intent.setCustomerId(customerId);
        intentMapper.insert(intent);
    }
}
