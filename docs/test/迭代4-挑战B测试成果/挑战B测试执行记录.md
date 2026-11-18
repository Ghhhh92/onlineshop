# 测试执行记录 · 迭代 4（挑战需求 B 全量回归）

- 负责人：叶文锦（测试）
- 日期：2026-11-18
- 后端执行方式：`cd backend && mvn clean test`（自动化，H2 内存库 + Flyway V1～V6 建表，无需 MySQL）
- 后端汇总：**Tests run: 262, Failures: 0, Errors: 0, Skipped: 0**（BUILD SUCCESS）
- 前端联调 E2E：`frontend/tests/challenge-b-e2e.cjs`（真实后端、无 mock）**PASS**，覆盖 14231～14237

## 一、本轮新增用例（叶文锦）

| 测试类 | 覆盖需求 | 用例数 | 说明 |
| --- | --- | --- | --- |
| `OrderQueryTest` | 14231 订单 | 9 | 直接下单参数校验、订单号格式、客户/卖家订单列表分页/倒序/状态筛选、订单详情快照、未登录校验 |

## 二、挑战 B 相关测试类明细

| 测试类 | 作者 | 覆盖需求 | 用例数 | 结果 |
| --- | --- | --- | --- | --- |
| `OrderStateMachineTest` | 高佳豪 | 14231～14233 | 12 | ✅ 通过 |
| `OrderLifecycleTest` | 高佳豪 | 14231～14233 | 10 | ✅ 通过（含卖家订单客户信息） |
| `CustomerShoppingTest` | 范与恒 | 14234～14237 | 18 | ✅ 通过 |
| `OrderQueryTest` | 叶文锦 | 14231 | 9 | ✅ 通过（本轮新增） |

- 全量构成：存量 252（挑战 A 213 + 挑战 B 后端 39）+ 高佳豪「订单客户信息」1 + 叶文锦本轮 9 = **262**。

## 三、前端联调 E2E（真实后端，无 mock）

- 脚本：`frontend/tests/challenge-b-e2e.cjs`（Playwright + 真实 Spring Boot + Vite dev server）
- 运行前提：
  1. `cd frontend && npm i` 安装依赖；首次运行需 `npx playwright install chromium` 下载浏览器
  2. 后端与 Vite dev server 已启动，并确认前端代理 `/api` 指向后端
  3. **数据库为空库**：脚本会新建商品与客户，并断言订单列表只有本次产生的订单，库里有历史订单时会断言失败
- 执行命令：`npm run test:e2e`（或 `node tests/challenge-b-e2e.cjs`）
- 结果：**PASS**（`publish -> favorite -> cart -> checkout -> 5-state order flow -> customer cancel -> cart-to-favorite; no page errors`）
- 覆盖链路：

| 步骤 | 需求 | 断言 |
| --- | --- | --- |
| 卖家发布商品 | — | 商品上架 |
| 客户注册 → 收藏 + 加入购物车 | 14234 / 14235 | 收藏页、购物车可见商品 |
| 购物车勾选去下单 | 14236 | 生成订单，进入「客户下单」 |
| 卖家订单管理五状态推进 | 14231 | 确认→备货→发货→完成，看到下单客户 |
| 客户查看已完成的订单 | 14231 | 「交易完成」，取消按钮消失 |
| 客户取消订单（未发货） | 14232 | 「已取消」 |
| 购物车多选转收藏 | 14237 | 购物车清空、收藏保留 |

## 四、结论

挑战需求 B 的 7 条需求（14231～14237）后端自动化用例全部覆盖并全量通过（262 用例），前端买家/卖家关键链路联调 E2E 通过，出口标准第 1、2 条满足。

已闭环：上一轮发现的「卖家订单详情缺客户信息」由高佳豪修复（`d2ef97f`）并补回归用例 `sellerOrderCarriesCustomerInfo`。
