package com.onlineshop.controller;

import com.onlineshop.mapper.SellerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 修改密码（US-12）：
 * 1. 已登录，提交新密码后修改成功；2. 新密码长度不足时拒绝；3. 修改成功后要用新密码登录。
 * 每个用例结束后回滚，admin 的密码恢复为 123456。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChangePasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("修改成功：当前登录失效，旧密码不能再登录，新密码可以登录")
    void changePasswordSuccess() throws Exception {
        MockHttpSession session = login("123456");

        changePassword(session, "123456", "newPass2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/seller/me").session(session)).andExpect(status().isUnauthorized());
        loginRequest("123456").andExpect(status().isBadRequest());
        loginRequest("newPass2026").andExpect(status().isOk());

        String stored = sellerMapper.selectByUsername("admin").getPassword();
        assertThat(passwordEncoder.matches("newPass2026", stored)).isTrue();
    }

    @Test
    @DisplayName("修改成功后，其他浏览器里的登录也失效")
    void otherSessionsInvalidated() throws Exception {
        MockHttpSession browserA = login("123456");
        MockHttpSession browserB = login("123456");

        changePassword(browserA, "123456", "newPass2026").andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/me").session(browserB))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录已失效，请重新登录"));
    }

    @Test
    @DisplayName("新密码少于 8 位：拒绝并提示，密码不变")
    void newPasswordTooShort() throws Exception {
        MockHttpSession session = login("123456");

        changePassword(session, "123456", "short1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("新密码长度为 8～64 个字符"));

        loginRequest("123456").andExpect(status().isOk());
    }

    @Test
    @DisplayName("原密码错误：拒绝")
    void wrongOldPassword() throws Exception {
        MockHttpSession session = login("123456");

        changePassword(session, "wrong-old", "newPass2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("原密码错误"));
    }

    @Test
    @DisplayName("新密码和原密码相同：拒绝")
    void sameAsOldPassword() throws Exception {
        MockHttpSession session = login("123456");
        changePassword(session, "123456", "newPass2026").andExpect(status().isOk());

        MockHttpSession again = login("newPass2026");
        changePassword(again, "newPass2026", "newPass2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("新密码不能和原密码相同"));
    }

    @Test
    @DisplayName("未登录不能修改密码")
    void requiresLogin() throws Exception {
        mockMvc.perform(put("/api/seller/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"newPass2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions changePassword(MockHttpSession session, String oldPassword, String newPassword)
            throws Exception {
        return mockMvc.perform(put("/api/seller/password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private ResultActions loginRequest(String password) throws Exception {
        return mockMvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpSession login(String password) throws Exception {
        return (MockHttpSession) loginRequest(password)
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
