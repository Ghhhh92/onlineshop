# 测试用例清单 · 迭代 3（挑战需求 A）

- 项目：在线购物系统（第 3 组）
- 负责人：叶文锦（测试）
- 范围：挑战需求 A 共 8 条 —— A.1-1 多商品 / A.1-2 二级类别 / A.1-3 库存自动下架 / A.1-4 浏览搜索 / A.1-5 多图富媒体 / A.2-1 客户注册 / A.2-2 历史下单 / A.2-3 商家看客户（对应 US-31 ～ US-38）
- 说明：后端自动化用例由李帆随 PR #11/#13/#14 写入，缺口部分由叶文锦补齐（新增 `SellerCategoryTest`、`CategoryTest`、`ProductSearchTest`、`CustomerOrderTest`，扩展 `ProductStockTest`）。每条验收标准逐一对照到自动化测试（`文件.方法`）。编号沿用 `TC-*`，执行后回填「实际结果 / 状态」。

## 0. 结论速览（8 条逐条对）

| 需求 | 对应故事 | 自动化覆盖 | 缺口处理 |
| --- | --- | --- | --- |
| A.1-1 多商品 | US-31 | ✅ 覆盖 | — |
| A.1-2 二级类别 | US-32 | ✅ 覆盖 | 已补 `CategoryTest` / `SellerCategoryTest` |
| A.1-3 库存自动下架 | US-33 | ✅ 覆盖 | 已补售罄流转与补货恢复断言 |
| A.1-4 浏览与搜索 | US-34 | ✅ 覆盖 | 已补 `ProductSearchTest` |
| A.1-5 多图与富媒体 | US-35 | ✅ 覆盖 | 详情页轮播/富文本渲染需 E2E |
| A.2-1 客户注册 | US-36 | ✅ 覆盖 | — |
| A.2-2 历史下单 | US-37 | ✅ 覆盖 | 已补 `CustomerOrderTest` |
| A.2-3 商家看客户 | US-38 | ✅ 覆盖 | — |

## 1. A.1-1 同时发布多个商品（US-31）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-MULTI-01 | 已有在售/交易中商品时可继续发布 | 已登录，库中已有在售 + 交易中商品 | 连续发布「第一件」「第二件」 | 两次均 HTTP 200，在售数量 +2，交易中商品状态不变 | ✅ `ProductPublishTest.publishMultipleProducts` |
| TC-MULTI-02 | 已下架商品不影响发布新商品 | 已登录，库中有已下架商品 | 发布「新商品」 | HTTP 200，正常上架 | ✅ `ProductPublishTest.publishAfterDelisted` |
| TC-MULTI-03 | 多件在售逐件浏览，按发布时间倒序 | 库中在售/交易中/已下架各若干 | GET /api/product/list | 只返回在售 + 交易中，按倒序；已下架不出现 | ✅ `ProductListTest.buyerListShowsOnSaleAndTrading` |
| TC-MULTI-04 | 一件都没有时显示空 | 库中仅已下架商品 | GET /api/product/list | total=0，records 空（前端「暂无在售商品」） | ✅ `ProductListTest.buyerListEmpty` |
| TC-MULTI-05 | 卖家后台看全部商品（含已下架） | 已登录 | GET /api/seller/product/list | 含已下架，按倒序，可按 status 筛选 | ✅ `ProductListTest.sellerList` |

## 2. A.1-2 商品二级类别（US-32）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-CAT-01 | 发布商品时选择二级类别 | 已登录 | POST publish 带 categoryId=5 | 商品 categoryId=5 | ✅ `ProductPublishTest.category` |
| TC-CAT-02 | 未选类别归「未分类」 | 已登录 | POST publish 不带 categoryId | categoryId 为空 | ✅ `ProductPublishTest.category` |
| TC-CAT-03 | 类别 id 非法（0/负）拒绝 | 已登录 | POST publish categoryId=0 | HTTP 400「类别不正确」 | ✅ `ProductPublishTest.category` |
| TC-CAT-04 | 买家按类别筛选 | 库中有不同类别商品 | GET /api/product/list?categoryId=1 | 只返回该类别的商品 | ✅ `ProductListTest.buyerListByCategory` |
| TC-CAT-05 | 类别树：一级下挂二级、最多两级 | 已建一级/二级类别 | GET /api/category/tree | 返回两级树结构，按 sort 升序 | ✅ `CategoryTest.twoLevelTree` / `emptyTree` / `orphanSecondLevelIgnored` |
| TC-CAT-06 | 新增类别（一级 / 挂到一级下的二级） | 已登录 | POST /api/seller/category | 一级 parentId=0，二级 parentId=一级 id；上级不存在/最多两级拒绝 | ✅ `SellerCategoryTest.createFirstLevel` / `createSecondLevel` / `thirdLevelRejected` / `missingParentRejected` |
| TC-CAT-07 | 类别重命名 / 调顺序 | 已登录 | PUT /api/seller/category/{id} | name、sort 更新成功 | ✅ `SellerCategoryTest.updateRenameAndSort` |
| TC-CAT-08 | 删除类别：有子类别或有商品时拒绝 | 已登录，类别下有子类别/商品 | DELETE /api/seller/category/{id} | HTTP 400 拒绝；空类别可删 | ✅ `SellerCategoryTest.deleteWithChildrenRejected` / `deleteWithProductsRejected` / `deleteEmptyCategory` |

## 3. A.1-3 库存与自动下架（US-33）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-STOCK-01 | 发布必填库存，范围 1～99999 | 已登录 | 分别缺 stock、stock=0、100000、99999 | 前三个 400（库存不能为空/至少为 1/不能超过 99999），99999 成功 | ✅ `ProductPublishTest.stockRequiredAndInRange` |
| TC-STOCK-02 | 交易成功扣减库存，不减成负数 | 商品 FROZEN、stock=2 | decreaseStock 三次 | 依次返回 1、0，第三次抛「库存不足」，库里为 0 | ✅ `ProductStockTest.decreaseStock` |
| TC-STOCK-03 | 扣减到 0 时商品自动转售罄 | 商品 ON_SALE、stock=1 | decreaseStock 到 0 | 库存 0，状态自动流转为 SALES_OUT | ✅ `ProductStockTest.decreaseStockSellsOutWhenReachesZero` |
| TC-STOCK-04 | 库存归零后不接新意向 | 商品 SALES_OUT | POST /api/buyer/intent | HTTP 400「商品已下架」 | ⚠️ 待补（依赖交易/冻结接口，迭代 4 接入 Controller 后补） |
| TC-STOCK-05 | 补货后直接恢复在售，无需重新发布 | 商品 SALES_OUT、stock=0 | PUT /stock {"stock":5} | HTTP 200，status 变 ON_SALE、stock=5 | ✅ `ProductStockTest.restockSalesOut` |
| TC-STOCK-06 | 在售/交易中补货只改库存 | 商品 ON_SALE / FROZEN | PUT /stock {"stock":5} | stock 更新，状态不变 | ✅ `ProductStockTest.restockOnSale` / `restockFrozen` |
| TC-STOCK-07 | 成交终态（已下架）不允许补货 | 商品 DELISTED | PUT /stock {"stock":5} | 抛「商品已下架，不能补货」，库存不变 | ✅ `ProductStockServiceTest.restockDelistedRejected` |
| TC-STOCK-08 | 改库存需登录 / 参数校验 | 未登录；缺 stock、0、100000 | PUT /stock | 未登录 401；非法值 400；不存在 404 | ✅ `ProductStockTest.updateStockRequiresLogin` / `updateStockValidation` |

## 4. A.1-4 浏览与搜索（US-34）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-SEARCH-01 | 首页浏览商品列表 | 库中多件在售 | GET /api/product/list | 倒序返回商品名/描述/图片/价格 | ✅ `ProductListTest.buyerListShowsOnSaleAndTrading` |
| TC-SEARCH-02 | 关键词匹配商品名称 | 库中「台灯」「小说」 | GET /api/product/list?keyword=台灯 | 只返回名称含「台灯」的商品 | ✅ `ProductSearchTest.searchByName` |
| TC-SEARCH-03 | 关键词匹配描述 | 库中某商品描述含关键词 | GET /api/product/list?keyword=关键词 | 返回描述含该关键词的商品 | ✅ `ProductSearchTest.searchByDescription` |
| TC-SEARCH-04 | 关键词与类别叠加筛选 | 库中多类别商品 | keyword + categoryId 同时传 | 交集结果 | ✅ `ProductSearchTest.searchCombinedWithCategory` |
| TC-SEARCH-05 | 无匹配结果提示 | 库中无匹配 | GET /api/product/list?keyword=不存在 | total=0（前端「没有找到商品」） | ✅ `ProductSearchTest.noMatch` |
| TC-SEARCH-06 | 搜索只返回在售/交易中，关键词去空格 | 库中含已下架同关键词商品 | keyword 搜索 | 已下架不出现；首尾空格忽略 | ✅ `ProductSearchTest.searchSkipsDelisted` / `keywordTrimmed` |

## 5. A.1-5 多图与富媒体描述（US-35）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-IMG-01 | 上传 JPG/PNG，按内容识别 | 已登录 | 上传 PNG、JPG | 返回 `/api/uploads/{32位}.png/.jpg`，按地址可取回原内容 | ✅ `ProductImageTest.uploadPng` / `uploadJpeg` |
| TC-IMG-02 | 改扩展名的 GIF/文本拒绝 | 已登录 | 上传伪 png/文本 | HTTP 400「只支持 JPG 或 PNG」 | ✅ `ProductImageTest.formatCheckedByContent` |
| TC-IMG-03 | 单张 ≤5MB 限制 | 已登录 | 正好 5MB、5MB+1 | 5MB 成功，超限 400「图片不能超过 5MB」 | ✅ `ProductImageTest.sizeLimit` |
| TC-IMG-04 | 未登录/空文件 | 未登录；空文件 | 上传 | 401「请先登录」/ 400「请选择要上传的图片」 | ✅ `ProductImageTest.requireLogin` / `missingFile` |
| TC-IMG-05 | 多张按顺序保存，第一张为封面 | 已登录 | 传 3 张，images 顺序打乱 | 封面=第一张，images 顺序与请求一致，详情/列表均带全图 | ✅ `ProductImageTest.publishWithMultipleImages` |
| TC-IMG-06 | 最多 9 张，重复地址只存一次 | 已登录 | 10 张、2 张同地址 | 10 张 400「最多 9 张图片」；重复只存 1 张 | ✅ `ProductImageTest.imageCountAndDuplicates` |
| TC-IMG-07 | 非上传接口返回的地址拒绝 | 已登录 | 外部 URL / 路径穿越 | HTTP 400「图片地址无效」 | ✅ `ProductImageTest.rejectsUnknownImageUrls` |
| TC-IMG-08 | 富文本过滤脚本 | 已登录 | 描述含 `<script>`、onclick | 脚本被去掉，保留排版/图片 | ✅ `ProductPublishTest.richTextDescription` + `RichTextSanitizerTest` |
| TC-IMG-09 | 详情页图片左右切换、富文本渲染 | 有图商品 | 前端打开详情页 | 轮播切换、`v-html` 正确渲染 | 🔧 手工/E2E（前端未就绪前先列，联调后补） |

## 6. A.2-1 客户自由注册（US-36）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-CUST-01 | 注册成功即登录，返回不含密码 | 无 | POST register 完整字段 | HTTP 200，返回 id/username/phone/defaultLocation，无 password，`/me` 可查 | ✅ `CustomerAuthTest.register` |
| TC-CUST-02 | 密码加密存储 | 无 | 注册后查库 | 库中非明文，BCrypt matches | ✅ `CustomerAuthTest.passwordEncrypted` |
| TC-CUST-03 | 用户名重复拒绝 | 已注册 zhangsan | 再注册 zhangsan | HTTP 400「用户名已被注册」 | ✅ `CustomerAuthTest.duplicateUsername` |
| TC-CUST-04 | 字段校验（用户名/密码/电话/地点） | 无 | 分别非法值 | 各返回对应 400 文案；用户名支持中文 | ✅ `CustomerAuthTest.registerValidation` / `chineseUsername` |
| TC-CUST-05 | 登录成功 / 用户名不存在与密码错同文案 | 已注册 | 正确、错密码、不存在 | 正确 200；后两者同 400「用户名或密码错误」 | ✅ `CustomerAuthTest.login` |
| TC-CUST-06 | 未登录 / 退出后查 me | 未登录、已退出 | GET /me | 均 401「请先登录」 | ✅ `CustomerAuthTest.meRequiresLogin` / `logout` |
| TC-CUST-07 | 客户与卖家两套登录互不干扰 | 已登录卖家/客户 | 交叉访问 | 各自 401 | ✅ `CustomerAuthTest.sellerAndCustomerSeparated` / `customerLogoutKeepsSeller` |

## 7. A.2-2 客户查看历史下单（US-37）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-ORDER-01 | 登录后可查「我的订单」 | 已注册并登录，名下有意向 | GET /api/customer/orders | HTTP 200，只返回本人订单 | ✅ `CustomerOrderTest.ordersDescWithStatus` / `onlyOwnOrders` |
| TC-ORDER-02 | 按时间倒序、含商品与交易结果 | 名下有成功/失败/排队订单 | GET /api/customer/orders | 倒序，statusText 成功/失败/排队中 | ✅ `CustomerOrderTest.ordersDescWithStatus` |
| TC-ORDER-03 | 失败记录也保留并标「失败」 | 名下有失败订单 | GET /api/customer/orders | 失败记录出现，标「失败」 | ✅ `CustomerOrderTest.ordersDescWithStatus` |
| TC-ORDER-04 | 未登录不可访问 | 未登录 | GET /api/customer/orders | HTTP 401「请先登录」 | ✅ `CustomerOrderTest.requireLogin` |

## 8. A.2-3 商家查看客户及购买历史（US-38）

| 用例编号 | 场景 | 前置条件 | 测试步骤 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- | --- | --- |
| TC-SELLCUST-01 | 客户列表（用户名/电话/地点/注册时间） | 已登录，注册 3 客户 | GET /api/seller/customer/list | 倒序，字段齐全，不含密码 | ✅ `SellerCustomerTest.customerList` |
| TC-SELLCUST-02 | 无客户时空列表 | 无客户 | GET /api/seller/customer/list | total=0，records 空 | ✅ `SellerCustomerTest.emptyCustomerList` |
| TC-SELLCUST-03 | 某客户购买历史（商品/下单时间/交易结果，含失败） | 该客户有成功/失败/排队订单 | GET /api/seller/customer/{id}/orders | 倒序，含商品名、价格、时间、状态 | ✅ `SellerCustomerTest.orders` |
| TC-SELLCUST-04 | 没下单客户空列表 / 客户不存在 404 | 无订单客户；id 不存在 | GET .../orders | 空数组 / 404「客户不存在」 | ✅ `SellerCustomerTest.ordersEmptyOrMissing` |
| TC-SELLCUST-05 | 卖家接口需卖家登录，客户登录也看不了 | 未登录 / 客户登录 | GET 列表与历史 | 均 401 | ✅ `SellerCustomerTest.requireSellerLogin` |

## 9. 状态机四态回归（US-23，横跨 A.1-3）

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-FSM-01 | 在售 → 售罄（库存归零） | 合法 | ✅ `ProductStateMachineTest`（10 用例含四态） |
| TC-FSM-02 | 售罄 → 在售（补货） | 合法 | ✅ `ProductStateMachineTest` |
| TC-FSM-03 | 已冻结 → 已下架（成交）/ 在售（回退） | 合法 | ✅ `ProductStateMachineTest` |
| TC-FSM-04 | 已下架 → 任何（非法终态） | 拒绝并保持原状 | ✅ `ProductStateMachineTest` |

## 10. 待办清单（剩余动作）

1. ~~补 US-32 类别接口测试~~ ✅ 已补：`CategoryTest`（树）、`SellerCategoryTest`（增删改与删除约束）。
2. ~~补 US-34 搜索接口测试~~ ✅ 已补：`ProductSearchTest`（名称/描述/叠加/无结果）。
3. ~~补 US-37 我的订单测试~~ ✅ 已补：`CustomerOrderTest`（登录校验/本人可见/倒序/失败记录）。
4. ~~补 US-33 状态流转集成断言~~ ✅ 已补：`ProductStockTest.decreaseStockSellsOutWhenReachesZero`、`restockSalesOut`。
5. **接口契约缺口**：`GET /api/customer/orders`（US-37 我的订单）未写入《接口契约-挑战A》，需提醒高佳豪补契约，前端林炜博按此对接。
6. **TC-STOCK-04**：库存归零后「不接新意向」的拒绝路径，等交易/冻结接口接入 Controller 后补用例。
7. 后端就绪后跑 `mvn clean test` 全量回归（存量 + 本轮新增 35 个），回填本清单；前端骨架齐后补 Playwright E2E（多图轮播、搜索、注册登录、我的订单）。
