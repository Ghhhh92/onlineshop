package com.onlineshop.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 卖家登录（US-11）：
 * 1. 正确的用户名和密码能登录；2. 密码错误登录失败并提示；3. 未登录不能访问后台功能。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerAuthTest {

    private static final String ADMIN = "{\"username\":\"admin\",\"password\":\"123456\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("用户名和密码正确：登录成功，返回卖家信息但不含密码")
    void loginSuccess() throws Exception {
        login(ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("密码错误：登录失败，提示用户名或密码错误")
    void wrongPassword() throws Exception {
        login("{\"username\":\"admin\",\"password\":\"wrong-password\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("用户名不存在：提示和密码错误一样，不暴露用户名是否存在")
    void unknownUser() throws Exception {
        login("{\"username\":\"nobody\",\"password\":\"123456\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("用户名为空：提示必填")
    void blankUsername() throws Exception {
        login("{\"username\":\"\",\"password\":\"123456\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    @DisplayName("登录成功后更换 Session ID")
    void sessionIdChangesAfterLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        mockMvc.perform(post("/api/seller/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN))
                .andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(before);
        assertThat(session.getAttribute("sellerId")).isNotNull();
    }

    @Test
    @DisplayName("未登录访问当前卖家：返回 401")
    void meWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/seller/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("登录后访问当前卖家：返回卖家信息")
    void meAfterLogin() throws Exception {
        mockMvc.perform(get("/api/seller/me").session(loggedInSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("未登录不能发布商品，登录后可以")
    void publishRequiresLogin() throws Exception {
        String product = "{\"name\":\"测试商品\",\"price\":10}";

        mockMvc.perform(post("/api/product/publish").contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/product/publish").session(loggedInSession())
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试商品"));
    }

    @Test
    @DisplayName("退出登录后再访问当前卖家：返回 401")
    void logout() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(post("/api/seller/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("买家接口不需要登录")
    void buyerApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/product/on-sale"))
                .andExpect(status().isOk());
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private MockHttpSession loggedInSession() throws Exception {
        return (MockHttpSession) login(ADMIN)
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
