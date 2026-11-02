-- V4 挑战需求 A 的表结构：商品库存和类别、类别表、商品图片表、客户表，意向关联客户

-- 商品库存：减到 0 时自动下架。已有的商品按 1 件计，已经下架（售出）的记为 0
ALTER TABLE product ADD COLUMN stock INT NOT NULL DEFAULT 1 COMMENT '库存数量';
UPDATE product SET stock = 0 WHERE status = 'DELISTED';

-- 商品所属类别：归到某个二级类别，为空表示未分类
ALTER TABLE product ADD COLUMN category_id BIGINT COMMENT '所属二级类别ID，为空表示未分类';

-- 按类别浏览商品，按发布时间倒序
CREATE INDEX idx_product_category_time ON product (category_id, create_time);

-- 类别：卖家自建，最多两级。一级类别的 parent_id 为 0，二级类别的 parent_id 是所属一级类别的 id
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL COMMENT '类别名称',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级类别ID，一级类别为 0',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '同级类别的展示顺序，小的在前',
    create_time DATETIME    NOT NULL COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品类别';

-- 同一个上级下类别名称不重复；按上级查下级类别也用这个索引
CREATE UNIQUE INDEX uk_category_parent_name ON category (parent_id, name);

-- 商品图片：一个商品可以有多张，按 sort 从小到大展示，第一张是封面。
-- product.image 继续存封面，商品列表不用再查这张表
CREATE TABLE IF NOT EXISTS product_image (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL COMMENT '商品ID',
    url        VARCHAR(500) NOT NULL COMMENT '图片地址',
    sort       INT          NOT NULL COMMENT '展示顺序，从 0 开始'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片';

CREATE INDEX idx_product_image_product ON product_image (product_id, sort);

-- 已有商品的图片搬进图片表
INSERT INTO product_image (product_id, url, sort)
SELECT id, image, 0 FROM product WHERE image IS NOT NULL AND image <> '';

-- 客户（注册的买家）：密码用 BCrypt 加密后保存
CREATE TABLE IF NOT EXISTS customer (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password         VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    phone            VARCHAR(20)  NOT NULL COMMENT '联系电话',
    default_location VARCHAR(100) NOT NULL COMMENT '默认交易地点',
    create_time      DATETIME     NOT NULL COMMENT '注册时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

-- 卖家查看客户列表，按注册时间倒序
CREATE INDEX idx_customer_create_time ON customer (create_time);

-- 客户登录后提交的意向记下客户 id，匿名提交的为空；历史下单、商家看客户购买历史都按它查
ALTER TABLE purchase_intent ADD COLUMN customer_id BIGINT COMMENT '提交意向的客户ID，匿名提交时为空';

CREATE INDEX idx_intent_customer_time ON purchase_intent (customer_id, create_time);
