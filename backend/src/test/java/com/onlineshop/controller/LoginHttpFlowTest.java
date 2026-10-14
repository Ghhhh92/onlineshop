package com.onlineshop.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动真实的内嵌 Tomcat，用会保存 Cookie 的 HTTP 客户端模拟浏览器：
 * 登录后浏览器靠 JSESSIONID Cookie 保持登录，退出后 Cookie 对应的会话失效。
 *
 * @author 李帆
 * @date 2026-10-11
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginHttpFlowTest {

    @LocalServerPort
    private int port;

    private final HttpClient browser = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

    @Test
    @DisplayName("登录 → 带 Cookie 访问后台 → 退出 → 再访问被拒绝")
    void loginKeepsSessionByCookie() throws Exception {
        HttpResponse<String> login = post("/api/seller/login", "{\"username\":\"admin\",\"password\":\"123456\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        String setCookie = String.join("; ", login.headers().allValues("Set-Cookie"));
        assertThat(setCookie).contains("JSESSIONID=").contains("HttpOnly").contains("SameSite=Lax");

        HttpResponse<String> me = get("/api/seller/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"username\":\"admin\"").doesNotContain("password");

        assertThat(post("/api/seller/logout", "").statusCode()).isEqualTo(200);

        assertThat(get("/api/seller/me").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return browser.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
