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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 发布商品（US-13）、价格约束（US-26），以及发布时的单件在售约束（US-14）。
 * 每个用例结束后回滚。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductPublishTest {

    private static final String DATE_TIME = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    private MockHttpSession seller;

    @BeforeEach
    void login() throws Exception {
        seller = (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    @DisplayName("填了名称和价格：发布成功，商品进入在售")
    void publishWithNameAndPrice() throws Exception {
        publish("{\"name\":\"二手键盘\",\"price\":12.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("二手键盘"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.statusText").value("在售"))
                .andExpect(jsonPath("$.data.createTime").value(matchesPattern(DATE_TIME)))
                .andExpect(content().string(containsString("\"price\":12.50")));
    }

    @Test
    @DisplayName("名称为空：提示必填")
    void blankName() throws Exception {
        publish("{\"name\":\"  \",\"price\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品名称不能为空"));
    }

    @Test
    @DisplayName("价格为空：提示必填")
    void missingPrice() throws Exception {
        publish("{\"name\":\"台灯\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("价格不能为空"));
    }

    @Test
    @DisplayName("描述和图片不填：允许发布，存为空")
    void descriptionAndImageOptional() throws Exception {
        publish("{\"name\":\"台灯\",\"price\":10,\"description\":\"\",\"image\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").isEmpty())
                .andExpect(jsonPath("$.data.image").isEmpty());
    }

    @Test
    @DisplayName("名称首尾的空格会去掉")
    void nameTrimmed() throws Exception {
        publish("{\"name\":\"  台灯  \",\"price\":10}")
                .andExpect(jsonPath("$.data.name").value("台灯"));
    }

    @Test
    @DisplayName("价格为 0 或负数：拒绝")
    void priceMustBePositive() throws Exception {
        publish("{\"name\":\"台灯\",\"price\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("价格必须大于 0"));
        publish("{\"name\":\"台灯\",\"price\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("价格必须大于 0"));
    }

    @Test
    @DisplayName("价格超过两位小数：拒绝并提示")
    void priceAtMostTwoDecimals() throws Exception {
        publish("{\"name\":\"台灯\",\"price\":12.345}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("价格最多两位小数，且不能超过 99999999.99"));
    }

    @Test
    @DisplayName("价格超过数据库能存的上限：拒绝")
    void priceTooLarge() throws Exception {
        publish("{\"name\":\"台灯\",\"price\":100000000}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("名称超过 100 个字符：拒绝")
    void nameTooLong() throws Exception {
        publish("{\"name\":\"" + "长".repeat(101) + "\",\"price\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品名称不能超过 100 个字符"));
    }

    @Test
    @DisplayName("已有在售商品时不能再发布")
    void secondPublishRejected() throws Exception {
        publish("{\"name\":\"第一件\",\"price\":10}").andExpect(status().isOk());

        publish("{\"name\":\"第二件\",\"price\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已有在售或交易中的商品，无法发布新商品"));
    }

    @Test
    @DisplayName("之前的商品已下架时可以发布新商品")
    void publishAfterDelisted() throws Exception {
        Product old = new Product();
        old.setName("已售出的商品");
        old.setPrice(new BigDecimal("50.00"));
        old.setStatus(ProductStatus.DELISTED);
        old.setCreateTime(LocalDateTime.of(2026, 10, 1, 10, 0));
        productMapper.insert(old);

        publish("{\"name\":\"新商品\",\"price\":10}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("发布时返回的价格、时间和之后查询到的完全一致")
    void publishedEqualsQueried() throws Exception {
        String published = publish("{\"name\":\"台灯\",\"price\":8}")
                .andReturn().getResponse().getContentAsString();
        String queried = mockMvc.perform(get("/api/product/on-sale"))
                .andReturn().getResponse().getContentAsString();

        assertThat(queried).isEqualTo(published);
    }

    private ResultActions publish(String body) throws Exception {
        return mockMvc.perform(post("/api/product/publish").session(seller)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
