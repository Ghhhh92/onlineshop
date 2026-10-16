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

MySQL 服务启动后，设置数据库密码并启动，不需要手动建库建表：

```powershell
cd backend

# PowerShell 设置数据库密码（只对当前窗口有效）
$env:DB_PASSWORD = "你的MySQL密码"

# 启动（端口 8000）
mvn spring-boot:run
```

首次启动会自动创建 `onlineshop` 库和表，并在卖家表为空时创建初始卖家账号 `admin / 123456`。之后重启数据都会保留。

数据库连接通过环境变量配置，不设置时用默认值：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/onlineshop?createDatabaseIfNotExist=true&...` | 数据库地址，库不存在时自动创建 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | 数据库密码 |

在 IntelliJ IDEA 里运行时，在运行配置的环境变量里填 `DB_PASSWORD=你的密码`。

## 运行后端测试

```bash
cd backend
mvn test
```

测试使用 H2 内存数据库（MySQL 兼容模式），不需要安装 MySQL。

## 修改表结构

表结构由 Flyway 管理，脚本在 `backend/src/main/resources/db/migration/`，启动时按版本号自动执行，每个脚本只执行一次。

- 改表时新增一个脚本，文件名按 `V{下一个版本号}__说明.sql`，例如 `V3__add_intent_requeue_column.sql`。
- 已经执行过的脚本不要再修改，否则启动时会校验失败。
- 已经建好表、但没有 Flyway 记录的库也可以直接启动：已有的表和数据保留，缺的表和索引会自动补上。

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

- 卖家登录接口 + 页面（李帆 / 范与恒）
- 发布商品接口 + 页面，含单件在售约束（李帆 / 范与恒 / 高佳豪）
- 商品状态机枚举与流转校验（高佳豪）
- 买家端首页展示在售商品（高佳豪 / 李帆）

## 迭代 2 待实现

- 买家购买意向提交与口令码（迭代 3）
- 排队与交易闭环（迭代 4）
- 历史商品、位次查询、撤销意向（迭代 5）
