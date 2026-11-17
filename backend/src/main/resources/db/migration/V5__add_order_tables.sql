-- V5 挑战需求 B 的订单表结构：订单主表与订单明细。
-- 购物车与收藏两张表由本轮后端另一条任务负责，另起 V6，不要写进本脚本。

CREATE TABLE IF NOT EXISTS customer_order (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(32)   NOT NULL COMMENT '订单号，展示与沟通用',
    customer_id  BIGINT        NOT NULL COMMENT '下单客户ID',
    status       VARCHAR(20)   NOT NULL COMMENT '状态：PENDING客户下单 / CONFIRMED商家确认 / PREPARED备货完成 / SHIPPED开始发货 / COMPLETED交易完成 / CANCELED已取消',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    create_time  DATETIME      NOT NULL COMMENT '下单时间',
    update_time  DATETIME      NOT NULL COMMENT '最后更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户订单';

CREATE UNIQUE INDEX uk_order_no ON customer_order (order_no);

-- 客户查自己的订单、卖家按状态查订单，都按时间倒序
CREATE INDEX idx_order_customer_time ON customer_order (customer_id, create_time);
CREATE INDEX idx_order_status_time ON customer_order (status, create_time);

-- 订单明细：商品名与单价是下单时的快照，商品后续改名改价不影响历史订单
CREATE TABLE IF NOT EXISTS customer_order_item (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL COMMENT '订单ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200)  NOT NULL COMMENT '下单时的商品名称',
    price        DECIMAL(10,2) NOT NULL COMMENT '下单时的单价',
    quantity     INT           NOT NULL COMMENT '数量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

CREATE INDEX idx_order_item_order ON customer_order_item (order_id);

-- 发货前按商品统计「已发货未完成」的数量，见 OrderService
CREATE INDEX idx_order_item_product ON customer_order_item (product_id);
