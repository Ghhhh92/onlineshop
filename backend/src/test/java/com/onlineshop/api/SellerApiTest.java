package com.onlineshop.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * 卖家登录接口测试（US-11）
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
class SellerApiTest extends BaseApiTest {

    @Test
    @DisplayName("TC-LOGIN-01 正确账号密码登录成功，不返回密码")
    void loginSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"123456\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.username", equalTo("admin"))
                .body("data.password", nullValue());
    }

    @Test
    @DisplayName("TC-LOGIN-02 用户名不存在返回 400")
    void loginUserNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"nobody\",\"password\":\"123456\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("用户名不存在"));
    }

    @Test
    @DisplayName("TC-LOGIN-03 密码错误返回 400")
    void loginWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"wrong\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("密码错误"));
    }

    @Test
    @DisplayName("TC-LOGIN-04 用户名为空触发参数校验")
    void loginBlankUsername() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"\",\"password\":\"123456\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("用户名不能为空"));
    }

    @Test
    @DisplayName("TC-LOGIN-05 密码为空触发参数校验")
    void loginBlankPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("密码不能为空"));
    }
}
