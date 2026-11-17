# 测试用例清单 · 迭代 4（挑战需求 B）

- 项目：在线购物系统（第 3 组）
- 负责人：叶文锦（测试）
- 范围：挑战需求 B 共 7 条 —— 订单管理与五状态流转 / 客户取消订单 / 商家取消订单 / 收藏商品 / 加入购物车与购物车管理 / 购物车多选下单 / 购物车多选转收藏（对应禅道 14231～14237）
- 说明：后端自动化用例由高佳豪（订单）与范与恒（收藏/购物车）随功能写入，叶文锦本轮做缺口补齐（新增 `OrderQueryTest` 9 个用例）并全量回归。每条验收标准逐一对照到自动化测试（`文件.方法`）。

## 0. 结论速览（7 条逐条对）

| 需求 | 禅道 | 自动化覆盖 | 缺口处理 |
| --- | --- | --- | --- |
| 订单管理与五状态流转 | 14231 | ✅ 覆盖 | 已补订单查询/校验（`OrderQueryTest`） |
| 客户取消订单 | 14232 | ✅ 覆盖 | — |
| 商家取消订单 | 14233 | ✅ 覆盖 | — |
| 收藏商品 | 14234 | ✅ 覆盖 | — |
| 加入购物车与购物车管理 | 14235 | ✅ 覆盖 | — |
| 购物车多选下单 | 14236 | ✅ 覆盖 | — |
| 购物车多选转收藏 | 14237 | ✅ 覆盖 | — |

## 1. 14231 订单管理与五状态流转

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-ORDER-01 | 五状态按序推进：客户下单→商家确认→备货完成→开始发货→交易完成 | 每步状态正确流转 | ✅ `OrderStateMachineTest.happyPath` / `OrderLifecycleTest.fullLifecycle` |
| TC-ORDER-02 | 不能跳过中间状态 | 拒绝，提示「非法状态流转」 | ✅ `OrderStateMachineTest.cannotSkipStates` |
| TC-ORDER-03 | 终态不可再流转 | 已取消/已完成不可再推进 | ✅ `OrderStateMachineTest.terminalStatesCannotTransit` |
| TC-ORDER-04 | 下单进入「客户下单」，名称单价为快照，不扣库存 | status=PENDING，库存不变 | ✅ `OrderLifecycleTest.placeOrder` |
| TC-ORDER-05 | 下单校验：已下架 / 库存不足整单拦截 | 400 具体文案 | ✅ `OrderLifecycleTest.placeRejectsInvalidProduct` |
| TC-ORDER-06 | 交易完成才扣库存，扣到 0 自动转售罄 | 库存正确扣减，0→售罄 | ✅ `OrderLifecycleTest.completeToZeroBecomesSalesOut` |
| TC-ORDER-07 | 开始发货只校验库存不扣减，不足则拒绝 | 停在备货完成 | ✅ `OrderLifecycleTest.shipRejectsWhenStockNotEnough` |
| TC-ORDER-08 | 直接下单参数校验（缺商品/缺数量/0/超 999） | 400 具体文案 | ✅ `OrderQueryTest.placeValidation`（本轮补） |
| TC-ORDER-09 | 订单号 = 时间到秒 + 4 位随机 | 18 位数字 | ✅ `OrderQueryTest.orderNoFormat`（本轮补） |
| TC-ORDER-10 | 订单列表分页/倒序/状态筛选、详情快照 | 见 §1.1 | ✅ `OrderQueryTest.customerList*` / `sellerList*` / `customerDetailSnapshot` / `sellerDetail`（本轮补） |

## 2. 14232 客户取消订单

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-CANCEL-C-01 | 开始发货前（下单/确认/备货）可取消 | 进入「已取消」 | ✅ `OrderStateMachineTest.customerCanCancelBeforeShipping` |
| TC-CANCEL-C-02 | 已发货/已完成/已取消不可取消 | 400「订单已发货，不能取消」 | ✅ `OrderStateMachineTest.customerCannotCancelAfterShipping` / `OrderLifecycleTest.customerCancelBoundary` |
| TC-CANCEL-C-03 | 只能取消本人订单 | 他人订单按 404 处理 | ✅ `OrderLifecycleTest.cannotTouchOthersOrder` |

## 3. 14233 商家取消订单

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-CANCEL-S-01 | 交易完成前（含已发货）可取消 | 进入「已取消」 | ✅ `OrderStateMachineTest.sellerCanCancelBeforeCompleted` / `OrderLifecycleTest.sellerCancelBoundary` |
| TC-CANCEL-S-02 | 交易完成后不可取消 | 400「订单已交易完成，不能取消」 | ✅ `OrderStateMachineTest.sellerCannotCancelCompleted` |

## 4. 14234 收藏商品

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-FAV-01 | 收藏幂等去重、取消收藏 | 重复收藏不新增，取消后移除 | ✅ `CustomerShoppingTest.favoriteIsIdempotentAndUsesContractPagination` |
| TC-FAV-02 | 收藏列表分页、按时间倒序、边界 | records/total，倒序 | ✅ `CustomerShoppingTest.favoritePaginationSortsNewestFirstAndClampsBounds` |
| TC-FAV-03 | 下架/删除商品仍可见且标记不可购 | delisted/available 正确 | ✅ `CustomerShoppingTest.productChangesAndRemovalRemainVisible` |
| TC-FAV-04 | 并发收藏不重复 | 去重 | ✅ `CustomerShoppingTest.concurrentAddsAndFavoritesDoNotLoseUpdatesOrDuplicate` |
| TC-FAV-05 | 收藏接口需客户登录 | 401 | ✅ `CustomerShoppingTest.allEndpointsRequireCustomerLogin` |

## 5. 14235 加入购物车与购物车管理

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-CART-01 | 加购默认 1、同商品累加 | 去重、数量累加 | ✅ `CustomerShoppingTest.cartDefaultsToOneAndAccumulates` |
| TC-CART-02 | 改数量、传 0 删除、删除条目 | 正确 | ✅ `CustomerShoppingTest.cartUpdateZeroDeletesAndDeleteWorks` |
| TC-CART-03 | 不能改/删他人购物车 | 400 | ✅ `CustomerShoppingTest.cannotMutateAnotherCustomersCart` |
| TC-CART-04 | 数量越界（0/负/1000/溢出）拒绝且不破坏原车 | 400，原车不变 | ✅ `CustomerShoppingTest.invalidQuantitiesAreRejectedAndOverflowPreservesCart` |
| TC-CART-05 | 缺商品/缺数量拒绝 | 400 | ✅ `CustomerShoppingTest.missingProductOrQuantityIsRejected` |
| TC-CART-06 | 失效/下架商品仍可见供移除 | delisted/available 正确 | ✅ `CustomerShoppingTest.productChangesAndRemovalRemainVisible` |

## 6. 14236 购物车多选下单

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-CHECKOUT-01 | 多选生成一个订单，只清选中项，下单不扣库存 | 单订单、快照、库存不变 | ✅ `CustomerShoppingTest.checkoutCreatesSingleOrderWithSnapshotsAndClearsOnlySelection` |
| TC-CHECKOUT-02 | 多商品下单用当前价格合计 | 金额正确 | ✅ `CustomerShoppingTest.checkoutMultipleProductsUsesCurrentPrices` |
| TC-CHECKOUT-03 | 库存不足 / 非在售整单拦截 | 400，购物车不变 | ✅ `CustomerShoppingTest.checkoutRejectsWholeSelectionForStockOrUnavailableProduct` |
| TC-CHECKOUT-04 | 非法/他人选中项拒绝且无部分写入 | 400/401，无部分写 | ✅ `CustomerShoppingTest.batchEndpointsRejectInvalidAndForeignSelectionsWithoutPartialWrites` |
| TC-CHECKOUT-05 | 并发结算只建一单 | 恰好一单 | ✅ `CustomerShoppingTest.simultaneousCheckoutsCreateOnlyOneOrder` |

## 7. 14237 购物车多选转收藏

| 用例编号 | 场景 | 预期结果 | 自动化映射 |
| --- | --- | --- | --- |
| TC-TOFAV-01 | 多选转收藏，已收藏跳过，只清选中项 | 去重、清选中 | ✅ `CustomerShoppingTest.transferSkipsExistingFavoritesAndClearsOnlySelectedItems` |
| TC-TOFAV-02 | 任一商品失效整单回滚 | 收藏/购物车均不变 | ✅ `CustomerShoppingTest.missingProductRollsBackTransferAndCheckout` |
| TC-TOFAV-03 | 清理失败时已落库数据一并回滚 | 无部分写 | ✅ `CustomerShoppingTest.cleanupFailureRollsBackOrderFavoritesAndPriorDeletes` |

## 8. 本轮缺口补齐（叶文锦）

新增 `OrderQueryTest`（9 个用例），补上此前未覆盖的订单查询与校验：

| 用例 | 覆盖点 |
| --- | --- |
| `placeValidation` | 直接下单 `@Valid` 参数校验（缺商品/缺数量/0/超 999） |
| `orderNoFormat` | 订单号 18 位数字格式 |
| `customerListDescAndOnlyOwn` | 我的订单倒序、只看本人 |
| `customerListFiltersByStatus` | 我的订单状态筛选 |
| `customerListPaginates` | 我的订单分页 |
| `customerDetailSnapshot` | 客户订单详情快照 |
| `sellerListAllAndFilter` | 卖家订单列表（全客户）+ 状态筛选 |
| `sellerDetail` | 卖家订单详情明细 |
| `requireLogin` | 订单列表/详情未登录 401 |

## 9. 待办清单与缺陷登记

1. ~~补订单查询/校验用例~~ ✅ 已补：`OrderQueryTest`（9 个用例）。
2. **🔴 契约不一致（待高佳豪确认）**：接口契约 §六 写「卖家订单详情含客户信息与明细」，但 `OrderDetail` DTO 只有 `id/orderNo/status/totalAmount/createTime/items`，**没有 customerId / username / phone**。卖家后台无法从订单详情得知下单客户是谁。需二选一：补代码（OrderDetail 带客户信息）或改契约，避免验收被挑。
3. **前端 E2E（被林炜博前端阻塞）**：仓库当前无 order/cart/favorite 的页面、路由与 api 封装，前端落地后补 `frontend/tests/challenge-b-e2e.cjs`（下单 → 商家推进 → 取消边界、收藏、加购 → 多选下单/转收藏）。
