package com.onlineshop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web 层配置：跨域只放行配置里的前端地址；不存在的接口返回 404。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("配置里的前端地址可以带 Cookie 跨域访问")
    void allowedOriginPassesPreflight() throws Exception {
        mockMvc.perform(options("/api/product/on-sale")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("其他网站的跨域请求被拒绝")
    void otherOriginRejected() throws Exception {
        mockMvc.perform(options("/api/product/on-sale")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("不存在的接口返回 404 和统一响应体")
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/api/not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("接口不存在"));
    }
}
