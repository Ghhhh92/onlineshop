package com.onlineshop.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 发布商品 + 单件在售约束接口测试（US-13 / US-14）
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
class ProductApiTest extends BaseApiTest {

    @Test
    @DisplayName("TC-PUB-01 正常发布商品，状态为在售、id 回填")
    void publishSuccess() {
        given()
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
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"\",\"price\":10.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("商品名称不能为空"));
    }

    @Test
    @DisplayName("TC-PUB-03a 价格为 null 触发参数校验")
    void publishNullPrice() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\"}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("价格不能为空"));
    }

    @Test
    @DisplayName("TC-PUB-03b 价格 <= 0 触发参数校验")
    void publishNonPositivePrice() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":0}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("价格必须大于 0"));
    }

    @Test
    @DisplayName("TC-PUB-04 已有在售商品时再发布被拒绝")
    void publishRejectedWhenOnSaleExists() {
        // 先发布第一件
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPhone\",\"price\":6999.00}")
                .when()
                .post("/product/publish")
                .then()
                .body("code", equalTo(200));

        // 再发布第二件应被拒
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"iPad\",\"price\":3999.00}")
                .when()
                .post("/product/publish")
                .then()
                .statusCode(200)
                .body("code", equalTo(400))
                .body("message", equalTo("已有在售或交易中的商品，无法发布新商品"));
    }
}
