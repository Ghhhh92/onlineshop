package com.onlineshop.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 发布商品 + 单件在售约束 + 登录权限接口测试（US-13 / US-14 / UC-07）
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
class ProductApiTest extends BaseApiTest {

    /**
     * 调用登录接口，提取 JSESSIONID 返回。后续受保护的请求用
     * {@code .cookie("JSESSIONID", sessionId)} 携带登录态。
     */
    private String loginAsAdmin() {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"123456\"}")
                .when()
                .post("/seller/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("JSESSIONID");
    }

    @Test
    @DisplayName("TC-PUB-01 正常发布商品，状态为在售、id 回填")
    void publishSuccess() {
        String sessionId = loginAsAdmin();
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"description\":\"手机\",\"price\":6999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.status", equalTo("ON_SALE"))
                .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("TC-PUB-02 名称为空触发参数校验")
    void publishBlankName() {
        String sessionId = loginAsAdmin();
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"\",\"price\":10.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(400)
                .body("message", equalTo("商品名称不能为空"));
    }

    @Test
    @DisplayName("TC-PUB-03a 价格为 null 触发参数校验")
    void publishNullPrice() {
        String sessionId = loginAsAdmin();
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\"}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(400)
                .body("message", equalTo("价格不能为空"));
    }

    @Test
    @DisplayName("TC-PUB-03b 价格 <= 0 触发参数校验")
    void publishNonPositivePrice() {
        String sessionId = loginAsAdmin();
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":0}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(400)
                .body("message", equalTo("价格必须大于 0"));
    }

    @Test
    @DisplayName("TC-PUB-04 已有在售商品时再发布被拒绝")
    void publishRejectedWhenOnSaleExists() {
        String sessionId = loginAsAdmin();
        // 先发布第一件
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":6999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200);

        // 再发布第二件应被拒
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPad\",\"price\":3999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(400)
                .body("message", equalTo("已有在售或交易中的商品，无法发布新商品"));
    }

    @Test
    @DisplayName("TC-PUB-05 未登录发布商品被拒绝，返回 401")
    void publishWithoutLoginRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":6999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(401)
                .body("code", equalTo(401))
                .body("message", equalTo("请先登录"));
    }

    @Test
    @DisplayName("TC-PUB-06 登录后退出的会话再发布被拒绝（登录态失效）")
    void publishRejectedWithInvalidatedSession() {
        String sessionId = loginAsAdmin();

        // 退出登录，销毁这个会话
        given()
                .cookie("JSESSIONID", sessionId)
                .when()
                .post("/seller/logout")
                .then()
                .statusCode(200);

        // 用已经失效的会话再发布，应被拒绝
        given()
                .cookie("JSESSIONID", sessionId)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":6999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(401)
                .body("message", equalTo("请先登录"));
    }
}
