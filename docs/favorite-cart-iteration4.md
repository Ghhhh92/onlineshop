# 迭代 4：收藏与购物车管理（范与恒）

任务：22637 收藏商品、22638 加入购物车与购物车管理、22639 购物车多选下单、22640 购物车多选转收藏。
实现依据：[接口契约-挑战B](https://github.com/Ghhhh92/onlineshop/wiki/接口契约-挑战B)，v2，2026-11-17。
基线：main / 96ab2c6。已包含本轮四项收藏、购物车后端任务。

## 接口

以下接口全部要求客户登录，复用 Session 和 `@RequireCustomerLogin`，卖家登录不能替代客户登录。
统一返回 `{code,message,data}`。写操作成功 `code=200,data=null`，参数或业务失败 HTTP 400，未登录 HTTP 401。

| 方法 | 路径 | 请求 |
| --- | --- | --- |
| GET | /api/customer/favorite?page=1&size=10 | 收藏倒序分页 |
| POST | /api/customer/favorite | `{"productId":1}`，幂等收藏 |
| DELETE | /api/customer/favorite/{productId} | 幂等取消收藏 |
| GET | /api/customer/cart | 当前客户全部购物车条目 |
| POST | /api/customer/cart | `{"productId":1,"quantity":2}`，省略 quantity 默认 1，重复加购累加 |
| PUT | /api/customer/cart/{id} | `{"quantity":3}`，0 删除，非零范围 1～999 |
| DELETE | /api/customer/cart/{id} | 删除当前客户条目 |

收藏 data 为 `{items,total,page,size}`，page 最小 1，size 范围 1～50；购物车 data 为数组。
两个列表的条目字段：

```json
{
  "id": 1,
  "productId": 1,
  "productName": "台灯",
  "price": 12.50,
  "image": "/uploads/lamp.jpg",
  "quantity": 2,
  "stock": 10,
  "status": "ON_SALE",
  "delisted": false,
  "available": true,
  "createTime": "2026-11-17 19:30:00"
}
```

收藏条目的 quantity 固定为 1；收藏取消使用 productId，购物车修改/删除使用条目 id。
商品资料实时读取，`available` 仅在商品在售且库存大于 0 时为 true。前端据此置灰不可购买的商品。
`delisted` 表示已下架或商品已删除；商品已删除时商品资料为 null，条目仍可删除。
收藏和购物车均按 createTime、id 倒序，时间相同时顺序稳定。

## 一致性与边界

- V6 新增 customer_cart、customer_favorite，客户与商品组合唯一；数量加数据库 CHECK 约束。
- 同一客户的写操作在事务中锁定 customer 行，避免并发首次加购重复或累加丢失；无需进程内锁。
- 修改和删除始终带 customer_id 条件，越权操作返回「购物车条目不存在」。
- 重复加购的合计超过 999 时整体拒绝，原条目保持不变。
- 加购、收藏只检查商品存在，不预占或扣减库存；数量可以大于当前库存。契约中的在售与库存校验由后续结算任务承担。
- 未修改 V1～V5 以及旧分页接口的 records 格式。

## 验证

新增 `CustomerShoppingTest`，覆盖登录、幂等、倒序分页、边界、数量累加、删除、用户隔离、商品失效以及并发写入。

```bash
cd backend
mvn -Dtest=CustomerShoppingTest test
mvn test
```

验证结果（2026-11-17，四项任务完成后）：购物车与收藏 18 项集成测试通过；完整后端回归 252 项通过，0 失败、0 错误、0 跳过。测试库为 H2（MySQL 兼容模式），V1～V6 迁移均执行成功。

## 多选下单和多选转收藏（22639、22640）

- `POST /api/customer/cart/checkout`
- `POST /api/customer/cart/to-favorite`

两个接口均要求客户登录，请求体相同：

```json
{"cartItemIds":[1,2]}
```

数组必填且非空，元素须为正整数，重复 ID 只处理一次。选择中包含其他客户的条目或已删除条目时，整批 HTTP 400，且不产生任何修改。

下单复用 `OrderService.place`，以当前商品单价生成一笔订单和多条快照明细；逐项检查在售及库存，任意商品不满足则整单拦截，并返回订单服务的错误原因。成功返回 `data` 为 `OrderDetail`，包含 `id`、`orderNo`、`status`、`totalAmount`、`items` 等。订单状态初始为 `PENDING`，下单不扣库存，扣减时机沿用订单服务。

转收藏跳过已收藏商品，成功返回 `data=null`。商品记录已删除时整批失败；仍存在但不可售的商品可以转收藏，前端按契约控制不可选状态。

成功后只移除本次选中的购物车条目，未选中的保留。订单创建、收藏写入及条目删除均处于同一个事务：即使条目清理时数据库失败，之前写入的订单、收藏以及已删除的条目也全部回滚。同一客户的购物车写操作串行，重复或并发结算相同条目只成功一次。

新增回归覆盖多商品下单和总额、重复 ID、重复提交、未选中条目保留、库存不足、非在售商品、失效条目、登录、越权、已有收藏去重、并发结算，并通过数据库触发器模拟清理失败验证事务回滚。
