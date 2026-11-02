package com.onlineshop.controller;

import com.onlineshop.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 客户注册登录（US-36）：注册、登录、当前客户、退出，客户登录和卖家登录互相独立。
 * 每个用例先清空客户，结束后回滚。
 *
 * @author 李帆
 * @date 2026-11-02
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerAuthTest {

    private static final String DATE_TIME = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM customer");
    }

    @Test
    @DisplayName("注册成功：返回客户信息、不含密码，注册后直接是登录状态")
    void register() throws Exception {
        MockHttpSession session = (MockHttpSession) register(body("zhangsan", "abc12345", "13800000001", " 图书馆门口 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.phone").value("13800000001"))
                .andExpect(jsonPath("$.data.defaultLocation").value("图书馆门口"))
                .andExpect(jsonPath("$.data.createTime").value(matchesPattern(DATE_TIME)))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/customer/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("zhangsan"));
    }

    @Test
    @DisplayName("密码加密后保存，库里存的不是明文")
    void passwordEncrypted() throws Exception {
        register(body("zhangsan", "abc12345", "13800000001", "图书馆门口")).andExpect(status().isOk());

        String saved = customerMapper.selectByUsername("zhangsan").getPassword();
        assertThat(saved).isNotEqualTo("abc12345");
        assertThat(passwordEncoder.matches("abc12345", saved)).isTrue();
    }

    @Test
    @DisplayName("用户名已被注册：提示不可用")
    void duplicateUsername() throws Exception {
        register(body("zhangsan", "abc12345", "13800000001", "图书馆门口")).andExpect(status().isOk());

        register(body("zhangsan", "another123", "13900000002", "食堂"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名已被注册"));
    }

    @Test
    @DisplayName("注册信息没填或格式不对：提示具体原因")
    void registerValidation() throws Exception {
        register("{\"password\":\"abc12345\",\"phone\":\"13800000001\",\"defaultLocation\":\"食堂\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写用户名"));
        register(body("z", "abc12345", "13800000001", "食堂"))
                .andExpect(jsonPath("$.message").value("用户名为 2～20 位中文、字母、数字或下划线"));
        register(body("zhang san", "abc12345", "13800000001", "食堂"))
                .andExpect(jsonPath("$.message").value("用户名为 2～20 位中文、字母、数字或下划线"));
        register(body("zhangsan", "abc123", "13800000001", "食堂"))
                .andExpect(jsonPath("$.message").value("密码长度为 8～64 个字符"));
        register(body("zhangsan", "abc12345", "12345", "食堂"))
                .andExpect(jsonPath("$.message").value("请输入 11 位手机号"));
        register(body("zhangsan", "abc12345", "13800000001", " "))
                .andExpect(jsonPath("$.message").value("请填写默认交易地点"));
        register(body("zhangsan", "abc12345", "13800000001", "地".repeat(101)))
                .andExpect(jsonPath("$.message").value("默认交易地点不能超过 100 个字符"));
    }

    @Test
    @DisplayName("用户名可以用中文")
    void chineseUsername() throws Exception {
        register(body("张三_01", "abc12345", "13800000001", "食堂"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("张三_01"));
    }

    @Test
    @DisplayName("登录：用户名和密码正确时成功；密码错误和用户名不存在提示同一句话")
    void login() throws Exception {
        register(body("zhangsan", "abc12345", "13800000001", "图书馆门口")).andExpect(status().isOk());

        login("zhangsan", "abc12345")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.defaultLocation").value("图书馆门口"));
        login("zhangsan", "wrong-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        login("nobody", "abc12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("没登录时查当前客户：返回 401")
    void meRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/customer/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("退出后再查当前客户：返回 401")
    void logout() throws Exception {
        MockHttpSession session = registeredSession("zhangsan");

        mockMvc.perform(post("/api/customer/logout").session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("卖家登录不算客户登录，客户登录也进不了卖家后台")
    void sellerAndCustomerSeparated() throws Exception {
        MockHttpSession seller = sellerSession();
        MockHttpSession customer = registeredSession("zhangsan");

        mockMvc.perform(get("/api/customer/me").session(seller)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/me").session(customer)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("同一个浏览器里客户退出，卖家的登录不受影响")
    void customerLogoutKeepsSeller() throws Exception {
        MockHttpSession session = sellerSession();
        register(body("zhangsan", "abc12345", "13800000001", "食堂")).andExpect(status().isOk());
        mockMvc.perform(post("/api/customer/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/customer/logout").session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/me").session(session)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/me").session(session)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Session 里的客户已经不存在：返回 401 登录已失效")
    void customerGone() throws Exception {
        MockHttpSession session = registeredSession("zhangsan");
        jdbcTemplate.update("DELETE FROM customer");

        mockMvc.perform(get("/api/customer/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录已失效，请重新登录"));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post("/api/customer/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/customer/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpSession registeredSession(String username) throws Exception {
        return (MockHttpSession) register(body(username, "abc12345", "13800000001", "食堂"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private MockHttpSession sellerSession() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/seller/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private static String body(String username, String password, String phone, String defaultLocation) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"phone\":\"" + phone
                + "\",\"defaultLocation\":\"" + defaultLocation + "\"}";
    }
}
