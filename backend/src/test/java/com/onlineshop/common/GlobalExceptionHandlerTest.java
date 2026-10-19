package com.onlineshop.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理：各类异常返回正确的 HTTP 状态码和统一响应体。
 *
 * @author 李帆
 * @date 2026-10-11
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("业务异常默认返回 400")
    void bizExceptionReturns400() throws Exception {
        mockMvc.perform(get("/probe/biz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("已有在售或交易中的商品"));
    }

    @Test
    @DisplayName("带 401 的业务异常返回 HTTP 401，前端据此跳转登录页")
    void bizExceptionWithCodeUsesThatStatus() throws Exception {
        mockMvc.perform(get("/probe/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("校验失败返回 400 和字段上的提示")
    void validationFailureReturns400() throws Exception {
        mockMvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("名称不能为空"));
    }

    @Test
    @DisplayName("请求体不是合法 JSON 返回 400")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{name:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));
    }

    @Test
    @DisplayName("缺少参数、参数类型不对返回 400")
    void badParameterReturns400() throws Exception {
        mockMvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少参数：id"));
        mockMvc.perform(get("/probe/param").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数格式不正确：id"));
    }

    @Test
    @DisplayName("请求方法不对返回 405")
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(post("/probe/biz"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    @DisplayName("未预料的异常返回 500，不把异常信息暴露给前端")
    void unexpectedExceptionHidesDetails() throws Exception {
        mockMvc.perform(get("/probe/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE))
                .andExpect(jsonPath("$.message", not(containsString("password"))));
    }

    @Test
    @DisplayName("Spring 自带状态码的 4xx 异常按原状态码返回，不当成 500")
    void springClientErrorKeepsItsStatus() throws Exception {
        mockMvc.perform(get("/probe/payload-too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(413))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.CLIENT_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Spring 自带状态码的 5xx 异常仍按服务器内部错误处理")
    void springServerErrorStillHidden() throws Exception {
        mockMvc.perform(get("/probe/unavailable"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/biz")
        void biz() {
            throw new BizException("已有在售或交易中的商品");
        }

        @GetMapping("/probe/unauthorized")
        void unauthorized() {
            throw new BizException(401, "请先登录");
        }

        @PostMapping("/probe/validate")
        void validate(@Valid @RequestBody ProbeRequest request) {
        }

        @GetMapping("/probe/param")
        void param(@RequestParam Long id) {
        }

        @GetMapping("/probe/crash")
        void crash() {
            throw new IllegalStateException("connect failed, password=secret");
        }

        @GetMapping("/probe/payload-too-large")
        void payloadTooLarge() {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }

        @GetMapping("/probe/unavailable")
        void unavailable() {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "db down");
        }
    }

    record ProbeRequest(@NotBlank(message = "名称不能为空") String name) {
    }
}
