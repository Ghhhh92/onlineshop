package com.onlineshop.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * REST Assured 接口测试基类：随机端口 + H2 测试库，每个用例前清空业务表。
 *
 * @author 叶文锦
 * @date 2026-10-13
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // 清空业务表，保证「单件在售约束」等用例相互隔离（seller 种子数据保留）
        jdbcTemplate.execute("DELETE FROM purchase_intent");
        jdbcTemplate.execute("DELETE FROM product");
    }
}
