package com.onlineshop.controller;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 买家凭口令码撤销意向（US-09），撤销后口令码失效（US-10）。
 * 每个用例先清空商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-31
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerIntentCancelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PurchaseIntentMapper intentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM purchase_intent");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("排队中的意向可以撤销，撤销后口令码失效")
    void cancelQueuing() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = submit("张三", "13800000001");

        cancel(code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertThat(intentMapper.selectByCode(code).getStatus()).isEqualTo(IntentStatus.CANCELED);
        query(code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("口令码已失效"));
    }

    @Test
    @DisplayName("撤销后，排在后面的人位次依次前移")
    void laterBuyersMoveForward() throws Exception {
        product(ProductStatus.ON_SALE);
        String first = submit("张三", "13800000001");
        String second = submit("李四", "13800000002");
        String third = submit("王五", "13800000003");

        cancel(first).andExpect(status().isOk());

        query(second).andExpect(jsonPath("$.data.position").value(1));
        query(third).andExpect(jsonPath("$.data.position").value(2));
    }

    @Test
    @DisplayName("交易中的意向不能撤销，状态保持交易中")
    void tradingCannotCancel() throws Exception {
        Product product = product(ProductStatus.FROZEN);
        intent(product, "TRADEAAA", 1, IntentStatus.TRADING);

        cancel("TRADEAAA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("正在交易中，不能撤销"));
        assertThat(intentMapper.selectByCode("TRADEAAA").getStatus()).isEqualTo(IntentStatus.TRADING);
    }

    @Test
    @DisplayName("交易已结束或已撤销的口令码不能撤销")
    void endedCannotCancel() throws Exception {
        Product product = product(ProductStatus.ON_SALE);
        intent(product, "SUCCESSA", 1, IntentStatus.SUCCESS);
        intent(product, "FAILEDAA", 2, IntentStatus.FAILED);
        intent(product, "CANCELAA", 3, IntentStatus.CANCELED);

        for (String code : new String[]{"SUCCESSA", "FAILEDAA", "CANCELAA"}) {
            cancel(code)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("口令码已失效"));
        }
    }

    @Test
    @DisplayName("同一个口令码撤销两次：第二次提示口令码已失效")
    void cancelTwice() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = submit("张三", "13800000001");

        cancel(code).andExpect(status().isOk());
        cancel(code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("口令码已失效"));
    }

    @Test
    @DisplayName("商品已下架时口令码失效，不能撤销")
    void delistedProductCannotCancel() throws Exception {
        Product product = product(ProductStatus.DELISTED);
        intent(product, "DELISTED", 1, IntentStatus.QUEUING);

        cancel("DELISTED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("口令码已失效"));
    }

    @Test
    @DisplayName("口令码不存在返回 404")
    void unknownCode() throws Exception {
        cancel("ZZZZZZZZ")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("口令码不存在"));
    }

    @Test
    @DisplayName("撤销时口令码不区分大小写、忽略前后空格")
    void codeIsNormalized() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = submit("张三", "13800000001");

        cancel("  " + code.toLowerCase(Locale.ROOT) + " ").andExpect(status().isOk());
        assertThat(intentMapper.selectByCode(code).getStatus()).isEqualTo(IntentStatus.CANCELED);
    }

    @Test
    @DisplayName("撤销后同一个手机号可以重新提交，排在队尾")
    void resubmitAfterCancel() throws Exception {
        product(ProductStatus.ON_SALE);
        String first = submit("张三", "13800000001");
        submit("李四", "13800000002");

        cancel(first).andExpect(status().isOk());

        String again = submit("张三", "13800000001");
        query(again).andExpect(jsonPath("$.data.position").value(2));
    }

    @Test
    @DisplayName("意向已不是期望的状态时，updateStatus 不修改")
    void updateStatusChecksCurrentStatus() {
        Product product = product(ProductStatus.FROZEN);
        PurchaseIntent trading = intent(product, "TRADEBBB", 1, IntentStatus.TRADING);

        assertThat(intentMapper.updateStatus(trading.getId(), IntentStatus.QUEUING, IntentStatus.CANCELED)).isFalse();
        assertThat(intentMapper.selectById(trading.getId()).getStatus()).isEqualTo(IntentStatus.TRADING);

        assertThat(intentMapper.updateStatus(trading.getId(), IntentStatus.TRADING, IntentStatus.SUCCESS)).isTrue();
        assertThat(intentMapper.selectById(trading.getId()).getStatus()).isEqualTo(IntentStatus.SUCCESS);
    }

    private String submit(String name, String phone) throws Exception {
        String body = mockMvc.perform(post("/api/buyer/intent").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.code");
    }

    private ResultActions cancel(String code) throws Exception {
        return mockMvc.perform(post("/api/buyer/intent/{code}/cancel", code));
    }

    private ResultActions query(String code) throws Exception {
        return mockMvc.perform(get("/api/buyer/intent/{code}", code));
    }

    private Product product(ProductStatus status) {
        Product product = new Product();
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 10, 31, 9, 0));
        productMapper.insert(product);
        return product;
    }

    private PurchaseIntent intent(Product product, String code, int position, IntentStatus status) {
        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getId());
        intent.setName("买家" + position);
        intent.setPhone("1390000000" + position);
        intent.setCode(code);
        intent.setPosition(position);
        intent.setStatus(status);
        intent.setCreateTime(LocalDateTime.of(2026, 10, 31, 10, position));
        intentMapper.insert(intent);
        return intent;
    }
}
