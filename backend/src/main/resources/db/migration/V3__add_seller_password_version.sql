-- V3 卖家密码版本号：每次修改密码加 1，修改之前的所有登录随之失效
ALTER TABLE seller ADD COLUMN password_version INT NOT NULL DEFAULT 0 COMMENT '密码版本号，修改密码时加 1';
