-- H2 测试库初始化（结构对齐 src/main/resources/schema.sql，去掉 MySQL 专属语法）
CREATE TABLE seller (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    image VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    finish_time TIMESTAMP
);

CREATE TABLE purchase_intent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    position INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL
);

-- 种子卖家：admin / 123456
INSERT INTO seller (username, password) VALUES ('admin', '123456');
