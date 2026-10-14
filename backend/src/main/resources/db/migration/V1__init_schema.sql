-- V1 初始表结构
-- 由 Flyway 在应用启动时执行，每个版本只执行一次；已经执行过的脚本不要再修改，改表请新增 V{n}__说明.sql。
-- 用 IF NOT EXISTS：库里已经存在的表原样保留，表里的数据不受影响。

-- 卖家表（单卖家）
CREATE TABLE IF NOT EXISTS seller (
    id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卖家';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL COMMENT '商品名称',
    description TEXT          COMMENT '商品描述',
    image       VARCHAR(500)  COMMENT '商品图片URL',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    status      VARCHAR(20)   NOT NULL COMMENT '状态：ON_SALE在售 / FROZEN已冻结 / DELISTED已下架',
    create_time DATETIME      NOT NULL COMMENT '发布时间',
    finish_time DATETIME      COMMENT '成交时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- 购买意向表
CREATE TABLE IF NOT EXISTS purchase_intent (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT      NOT NULL COMMENT '商品ID',
    name        VARCHAR(50) NOT NULL COMMENT '买家姓名',
    phone       VARCHAR(20) NOT NULL COMMENT '联系电话',
    code        VARCHAR(50) NOT NULL UNIQUE COMMENT '唯一口令码',
    position    INT         NOT NULL COMMENT '排队位次',
    status      VARCHAR(20) NOT NULL COMMENT '状态：QUEUING排队中 / TRADING交易中 / SUCCESS成功 / FAILED失败 / CANCELED已撤销',
    create_time DATETIME    NOT NULL COMMENT '提交时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购买意向';
