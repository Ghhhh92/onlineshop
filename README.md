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

## Docker 一键部署（推荐）

无需安装 MySQL，也无需本地构建：

```bash
# 有源码时：构建并启动（MySQL 8 + 后端 + 前端）
docker compose up -d --build

# 仅有离线镜像包时：先导入镜像，再启动（无需联网）
docker load -i onlineshop-images.tar
docker compose -f docker-compose.deploy.yml up -d
```

启动后：

| 服务 | 地址 |
| --- | --- |
| 买家端首页 | http://localhost:8080 |
| 卖家后台登录 | http://localhost:8080/seller/login（初始账号 `admin / 123456`） |
| 后端接口 | http://localhost:8000/api |
| MySQL | 宿主机 `13306` → 容器 `3306`（避开本机可能已占用的 3306） |

更详细的镜像构建、镜像导出与常见问题见 Wiki [安装部署手册](https://github.com/Ghhhh92/onlineshop/wiki/安装部署手册)。

## 运行后端测试

```bash
cd backend
mvn clean test
```

共 103 个自动化用例（单元 + 接口，H2 内存库，无需 MySQL），成功标志为 `Tests run: 103, Failures: 0, Errors: 0` + `BUILD SUCCESS`。执行说明见 Wiki [构建过程手册](https://github.com/Ghhhh92/onlineshop/wiki/构建过程手册) 第 7 节。

## 迭代 2 已实现

- 卖家登录接口 + 页面（李帆 / 范与恒）
- 发布商品接口 + 页面，含单件在售约束（李帆 / 范与恒 / 高佳豪）
- 商品状态机枚举与流转校验（高佳豪）
- 买家端首页展示在售商品，商品交易中时照常展示（高佳豪 / 李帆）
- 买家提交购买意向 + 口令码查询，卖家查看意向人 / 当前商品 / 历史商品（李帆 / 林炜博 / 范与恒；原规划迭代 3，为满足基线需求验收提前交付）
- Docker 化：后端 / 前端镜像、离线镜像包与一键部署（高佳豪）
- 自动化测试：103 个后端用例 + 真实端到端回归脚本（叶文锦）

## 迭代 2 待实现

- 排队与交易闭环：进交易自动冻结、标记成交 / 失败、失败后重新排队（迭代 4）
- 手动冻结 / 解冻（迭代 4）
- 撤销 / 修改意向（迭代 5）
