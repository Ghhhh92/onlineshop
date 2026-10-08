# 在线购物系统（onlineshop）

单卖家、单件在售的在线购物系统。买家免注册，凭姓名 + 电话提交购买意向并获得唯一口令码；多意向「先到先得」排队，队首进入线下交易时商品自动冻结，交易结果由卖家后台标记。

## 技术栈

- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8
- 前端：Vue 3 + Vite + Vue Router 4 + Pinia + Axios + Element Plus

## 目录结构

```
onlineshop/
├── backend/          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/onlineshop/
│       ├── OnlineshopApplication.java
│       ├── common/       # 统一返回体、异常处理
│       ├── config/       # 跨域等配置
│       ├── controller/   # REST 接口
│       ├── dto/          # 请求/响应对象
│       ├── entity/       # 数据库实体
│       ├── enums/        # 状态机枚举（高佳豪）
│       ├── mapper/       # 数据访问
│       └── service/      # 业务逻辑
└── frontend/         # Vue 3 前端
    ├── package.json
    └── src/
        ├── api/        # 接口封装
        ├── router/     # 路由 + 登录守卫
        ├── stores/     # Pinia 状态
        ├── utils/      # Axios 封装
        └── views/
            ├── buyer/   # 买家端页面（林炜博）
            └── seller/  # 卖家后台页面（范与恒）
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0

## 启动后端

```bash
cd backend

# 1. 创建数据库并执行初始化脚本
mysql -u root -p < src/main/resources/schema.sql

# 2. 修改 application.yml 中的数据库账号密码

# 3. 启动（端口 8000）
mvn spring-boot:run
```

初始卖家账号：`admin / 123456`

## 启动前端

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器（端口 5173，已配置 /api 代理到后端 8000）
npm run dev
```

访问 http://localhost:5173

## 迭代 2 已实现

- 卖家登录接口 + 页面（李帆 / 林炜博）
- 发布商品接口 + 页面，含单件在售约束（李帆 / 范与恒 / 高佳豪）
- 商品状态机枚举与流转校验（高佳豪）
- 买家端首页展示在售商品（林炜博）

## 迭代 2 待实现

- 买家购买意向提交与口令码（迭代 3）
- 排队与交易闭环（迭代 4）
- 历史商品、位次查询、撤销意向（迭代 5）
