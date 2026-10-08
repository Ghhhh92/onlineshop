-- 在线购物系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS onlineshop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE onlineshop;

-- 卖家表（单卖家）
CREATE TABLE IF NOT EXISTS seller (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卖家';

-- 初始卖家账号：admin / 123456
INSERT INTO seller (username, password) VALUES ('admin', '123456');

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    image VARCHAR(500) COMMENT '商品图片URL',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    status VARCHAR(20) NOT NULL COMMENT '状态：ON_SALE在售 / FROZEN已冻结 / DELISTED已下架',
    create_time DATETIME NOT NULL COMMENT '发布时间',
    finish_time DATETIME COMMENT '成交时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- 购买意向表
CREATE TABLE IF NOT EXISTS purchase_intent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    name VARCHAR(50) NOT NULL COMMENT '买家姓名',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '唯一口令码',
    position INT NOT NULL COMMENT '排队位次',
    status VARCHAR(20) NOT NULL COMMENT '状态：QUEUING排队中 / TRADING交易中 / SUCCESS成功 / FAILED失败 / CANCELED已撤销',
    create_time DATETIME NOT NULL COMMENT '提交时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购买意向';
