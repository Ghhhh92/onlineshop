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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 买家提交购买意向（US-04、US-05、US-29、US-30）与凭口令码查询（US-06、US-07、US-10）。
 * 每个用例先清空商品和意向，结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerIntentTest {

    private static final String CODE_PATTERN = "^[A-HJ-NP-Z2-9]{8}$";

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
    @DisplayName("没有在售商品：拒绝并提示暂无在售商品")
    void noProductOnSale() throws Exception {
        submit("张三", "13800000001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("暂无在售商品"));
    }

    @Test
    @DisplayName("商品已冻结（交易中）：拒绝并提示商品交易中")
    void frozenProductRejects() throws Exception {
        product(ProductStatus.FROZEN);

        submit("张三", "13800000001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品交易中"));
    }

    @Test
    @DisplayName("只填姓名和电话、不登录即可提交，当场拿到 8 位口令码和位次")
    void submitSuccess() throws Exception {
        Product product = product(ProductStatus.ON_SALE);

        submit("  张三  ", "13800000001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(matchesPattern(CODE_PATTERN)))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.createTime").value(matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")));
        submit("李四", "13800000002")
                .andExpect(jsonPath("$.data.position").value(2));

        PurchaseIntent saved = intentMapper.selectByProductInOrder(product.getId()).get(0);
        assertThat(saved.getName()).isEqualTo("张三");
        assertThat(saved.getStatus()).isEqualTo(IntentStatus.QUEUING);
    }

    @Test
    @DisplayName("姓名、电话为空或电话格式不对：提示具体原因")
    void validation() throws Exception {
        product(ProductStatus.ON_SALE);

        submit(" ", "13800000001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写姓名"));
        submit("张三", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入 11 位手机号"));
        mockMvc.perform(post("/api/buyer/intent").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"张三\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写联系电话"));
    }

    @Test
    @DisplayName("同一手机号还在排队时不能重复提交，撤销后可以再提交")
    void duplicatePhone() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = codeOf(submit("张三", "13800000001"));

        submit("张三", "13800000001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该手机号已在排队中"));

        setStatus(code, IntentStatus.CANCELED);
        submit("张三", "13800000001").andExpect(status().isOk());
    }

    @Test
    @DisplayName("凭口令码查到位次、状态和自己的信息，电话中间打码，口令码不区分大小写")
    void queryByCode() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = codeOf(submit("张三", "13800001234"));

        mockMvc.perform(get("/api/buyer/intent/" + code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.status").value("QUEUING"))
                .andExpect(jsonPath("$.data.statusText").value("排队中"))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("138****1234"))
                .andExpect(jsonPath("$.data.productName").value("测试商品"));
    }

    @Test
    @DisplayName("前面有人撤销后，后面的人位次自动前移")
    void positionMovesForward() throws Exception {
        product(ProductStatus.ON_SALE);
        String first = codeOf(submit("甲", "13800000001"));
        codeOf(submit("乙", "13800000002"));
        String third = codeOf(submit("丙", "13800000003"));

        setStatus(first, IntentStatus.CANCELED);

        mockMvc.perform(get("/api/buyer/intent/" + third))
                .andExpect(jsonPath("$.data.position").value(2));
    }

    @Test
    @DisplayName("进入交易后能查到「交易中」")
    void queryTrading() throws Exception {
        product(ProductStatus.ON_SALE);
        String code = codeOf(submit("张三", "13800000001"));
        setStatus(code, IntentStatus.TRADING);

        mockMvc.perform(get("/api/buyer/intent/" + code))
                .andExpect(jsonPath("$.data.status").value("TRADING"))
                .andExpect(jsonPath("$.data.statusText").value("交易中"));
    }

    @Test
    @DisplayName("口令码不存在：返回 404")
    void unknownCode() throws Exception {
        mockMvc.perform(get("/api/buyer/intent/ABCDEFGH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("口令码不存在"));
    }

    @Test
    @DisplayName("交易结束或商品下架后，口令码失效")
    void endedCodeInvalid() throws Exception {
        Product product = product(ProductStatus.ON_SALE);
        String success = codeOf(submit("甲", "13800000001"));
        String queuing = codeOf(submit("乙", "13800000002"));

        setStatus(success, IntentStatus.SUCCESS);
        mockMvc.perform(get("/api/buyer/intent/" + success))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("口令码已失效"));

        product.setStatus(ProductStatus.DELISTED);
        productMapper.updateById(product);
        mockMvc.perform(get("/api/buyer/intent/" + queuing))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("口令码已失效"));
    }

    private Product product(ProductStatus status) {
        Product product = new Product();
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 10, 19, 9, 0));
        productMapper.insert(product);
        return product;
    }

    private ResultActions submit(String name, String phone) throws Exception {
        return mockMvc.perform(post("/api/buyer/intent").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}"));
    }

    private static String codeOf(ResultActions result) throws Exception {
        String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"code\":\"([A-Z0-9]{8})\".*", "$1");
    }

    private void setStatus(String code, IntentStatus status) {
        PurchaseIntent intent = intentMapper.selectByCode(code);
        intent.setStatus(status);
        intentMapper.updateById(intent);
    }
}
