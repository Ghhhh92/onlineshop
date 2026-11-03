# 测试执行记录 · 迭代 3（挑战需求 A 全量回归）

- 负责人：叶文锦（测试）
- 日期：2026-11-03
- 执行方式：`cd backend && mvn clean test`（自动化，H2 内存库 + Flyway V1～V4 建表，无需 MySQL）
- 汇总：**Tests run: 212, Failures: 0, Errors: 0, Skipped: 0**（BUILD SUCCESS）
- 前端联调 E2E：`frontend/tests/challenge-a-e2e.cjs`（真实后端 H2、无 mock）**PASS**，覆盖 US-31/34/35/36/37

## 一、本轮新增 / 扩展用例（叶文锦）

| 测试类 | 覆盖需求 | 用例数 | 说明 |
| --- | --- | --- | --- |
| `CategoryTest` | US-32 类别树 | 4 | 两级树结构、排序、孤儿节点、免登录 |
| `SellerCategoryTest` | US-32 类别管理 | 13 | 新增一级/二级、最多两级、同名、删除约束、登录校验 |
| `ProductSearchTest` | US-34 搜索 | 6 | 名称/描述匹配、叠加类别、无结果、去空格、跳过下架 |
| `CustomerOrderTest` | US-37 我的订单 | 4 | 登录校验、只看本人、倒序、失败记录保留 |
| `ProductStockTest`（扩展） | US-33 库存 | +2 | 扣减到 0 自动转售罄、售罄补货恢复在售 |

- 本轮合计新增 / 扩展 **29** 个用例，全部通过。
- 全量构成：存量（含李帆本轮后端测试）183 + 叶文锦本轮 29 = **212**。

## 二、全量回归结果明细

| 测试类 | 用例数 | 结果 |
| --- | --- | --- |
| `api.ProductApiTest` | 7 | ✅ 通过 |
| `api.SellerApiTest` | 5 | ✅ 通过 |
| `common.GlobalExceptionHandlerTest` | 10 | ✅ 通过 |
| `common.RichTextSanitizerTest` | 6 | ✅ 通过 |
| `config.DatabaseMigrationTest` | 7 | ✅ 通过 |
| `config.SellerAccountInitializerTest` | 3 | ✅ 通过 |
| `config.WebConfigTest` | 3 | ✅ 通过 |
| `controller.BuyerHomeTest` | 3 | ✅ 通过 |
| `controller.BuyerIntentCancelTest` | 10 | ✅ 通过 |
| `controller.BuyerIntentTest` | 14 | ✅ 通过 |
| `controller.CategoryTest` | 4 | ✅ 通过（本轮新增） |
| `controller.ChangePasswordTest` | 6 | ✅ 通过 |
| `controller.CustomerAuthTest` | 11 | ✅ 通过 |
| `controller.CustomerOrderTest` | 4 | ✅ 通过（本轮新增） |
| `controller.LoginHttpFlowTest` | 1 | ✅ 通过 |
| `controller.ProductImageTest` | 12 | ✅ 通过 |
| `controller.ProductListTest` | 8 | ✅ 通过 |
| `controller.ProductPublishTest` | 16 | ✅ 通过 |
| `controller.ProductSearchTest` | 6 | ✅ 通过（本轮新增） |
| `controller.ProductStockTest` | 8 | ✅ 通过（含本轮 +2） |
| `controller.SellerAuthTest` | 10 | ✅ 通过 |
| `controller.SellerCategoryTest` | 13 | ✅ 通过（本轮新增） |
| `controller.SellerCustomerTest` | 5 | ✅ 通过 |
| `controller.SellerProductQueryTest` | 7 | ✅ 通过 |
| `mapper.ChallengeSchemaMapperTest` | 5 | ✅ 通过 |
| `mapper.ProductMapperTest` | 4 | ✅ 通过 |
| `service.BuyerIntentConcurrencyTest` | 2 | ✅ 通过 |
| `service.IntentCodeGeneratorTest` | 2 | ✅ 通过 |
| `service.ProductServiceTest` | 4 | ✅ 通过 |
| `service.ProductStockServiceTest` | 3 | ✅ 通过 |
| `service.SellerServiceTest` | 3 | ✅ 通过 |
| `statemachine.ProductStateMachineTest`（合法/非法流转） | 10 | ✅ 通过 |

## 三、前端联调 E2E（真实后端，无 mock）

- 脚本：`frontend/tests/challenge-a-e2e.cjs`（Playwright + 真实 Spring Boot + H2 内存库 + Vite dev server）
- 结果：**PASS**（`multi-product -> search -> multi-image detail -> register -> submit intent -> my orders; no page errors`）
- 覆盖链路：

| 步骤 | 需求 | 断言 |
| --- | --- | --- |
| 卖家登录 + 发布 2 件商品 | US-31 多商品 | 买家首页同时看到「E2E 台灯」「E2E 小说」 |
| 关键词搜索「台灯」 | US-34 浏览搜索 | 只显示「E2E 台灯」，「E2E 小说」被过滤 |
| 商品详情多图轮播 + 描述 | US-35 多图富媒体 | 轮播含 2 张不同图片、描述文本渲染 |
| 客户注册（自动登录） | US-36 客户注册 | 注册后跳转「我的订单」，显示空状态 |
| 登录态提交购买意向 | US-37 历史下单 | 生成 8 位口令码，排第 1 位 |
| 「我的订单」查看 | US-37 历史下单 | 出现「E2E 台灯」与「排队中」状态 |

## 四、结论

挑战需求 A 的 8 条需求（US-31～US-38）后端自动化用例全部覆盖并全量通过（212 用例），前端买家侧关键链路联调 E2E 通过。出口标准第 2 条「后端自动化用例全绿」满足。

剩余待办：接口契约补齐 `GET /api/customer/orders`（高佳豪）；缺陷 DEFECT-01「售罄商品仍可提交意向」待李帆修复（详见《挑战A测试用例清单》§11）。
