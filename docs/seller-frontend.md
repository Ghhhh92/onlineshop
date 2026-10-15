# 卖家后台前端交付（范与恒）

范围对应 Wiki《组内分工表》中的卖家后台四页面。仅修改前端，不修改其他成员负责的后端与买家页面。

## 已完成

- `/seller/login`：用户名密码校验、回车提交、重复提交保护、失败提示、成功进入后台。
- `/seller/admin`：商品图片、价格、描述、发布时间、在售/冻结状态；意向人姓名、电话、提交时间、位次、结果，提交时间升序。
- `/seller/publish`：必填、纯空白名称、长度和价格范围校验；可选 HTTP/HTTPS 图片及预览；发布前检查单件在售限制；后端最终校验冻结/并发发布；成功返回后台。
- `/seller/history`：历史列表分页、成交时间、商品明细及意向人结果，只读展示。
- 共用导航、移动端布局、退出登录、修改密码；路由通过 `/seller/me` 恢复服务端会话，刷新页面无需再次登录，401 后清理状态。
- 加载失败与“无数据”分开显示，查询可刷新重试。

## 后端联调边界

现有后端已接通：登录、查询当前卖家、退出、改密、发布商品、查询在售商品。
以下是**建议接口约定，尚未在主干后端实现**，集中在 `frontend/src/api/sellerWorkspace.js`。与李帆确认后可在该文件调整路径与参数：

| 方法 / 路径 | data 格式 | 说明 |
| --- | --- | --- |
| GET `/api/seller/product/current` | `ProductInfo` 或 `null` | 当前在售或冻结商品 |
| GET `/api/seller/product/{id}/intents` | `[{id,name,phone,position,status,createTime}]` | 当前/历史商品意向人，含失败、撤销结果 |
| GET `/api/seller/product/history?page=1&size=10` | `{records:[ProductInfo],total:11}` | 从 1 开始分页；后端按 finishTime 倒序、id 倒序返回 |

统一响应格式沿用 `{code:200,message:"success",data:...}`。查询接口应要求卖家登录，不返回口令码等不必要字段。

当前商品接口 404/405 时降级调用 `/api/product/on-sale`，明确提示冻结状态不可用，不将冻结商品误报为“无当前商品”。意向人及历史接口 404/405 显示暂不可用，不填充假数据。其他错误显示加载失败。未来接口返回空数组时才显示暂无记录。

**后端未补齐前，意向人/历史/冻结状态不能进行真实端到端验收。** 冻结/解冻、交易成功/失败操作属于后续交易接口联调范围，本次未虚构可用按钮。

## 运行与验证

```bash
cd frontend
npm install
npm run dev
npm run build
```

前端 http://localhost:5173，后端 http://localhost:8000。已有初始卖家账号见根目录 README。

隔离浏览器冒烟脚本 `frontend/tests/seller-smoke.cjs` 使用模拟 HTTP 响应，不读写真实卖家数据库。启动前端后，单独安装并运行 Playwright：

```bash
npm install --no-save playwright
npx playwright install chromium --only-shell
node tests/seller-smoke.cjs
```

覆盖未登录拦截、空表单、密码错误、成功登录、刷新恢复、冻结禁止发布、真实发布请求内容、意向人排序、历史分页和明细、缺失接口、改密后重新登录、退出、移动端宽度、页面运行时错误。模拟接口通过不代表后端接口已实现。

## 本地验证结果（2026-10-15）

- Vite 生产构建通过（有 Element Plus 主包体积提示，不影响构建）。
- Playwright 隔离冒烟通过，覆盖以上交互，未发现页面运行时异常。
- 已检查 390px 手机布局截图，无页面横向溢出；意向人表格可横向滚动查看所有列。
- 已启动真实 Spring Boot 后端（H2 文件数据库），通过前端代理验证在售查询、登录、当前卖家会话及退出接口均返回 200。
- 未运行 MySQL 联调，缺失查询接口仍需后端成员实现。
