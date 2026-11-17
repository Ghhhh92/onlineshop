-- 挑战 B：收藏与购物车。客户和商品的组合唯一，避免重复条目。
CREATE TABLE customer_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_cart_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT ck_cart_quantity CHECK (quantity BETWEEN 1 AND 999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE customer_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    CONSTRAINT uk_favorite_customer_product UNIQUE (customer_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_favorite_customer_time ON customer_favorite (customer_id, create_time, id);
