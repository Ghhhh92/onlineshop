// Real end-to-end regression for Challenge B (orders / favorites / cart), no mock:
// hits a real Spring Boot backend through the Vite dev proxy (/api -> 127.0.0.1:8000).
// Assumes a freshly started backend (empty DB) and Vite dev server on localhost:5173.
//   Usage: node tests/challenge-b-e2e.cjs   (E2E_BASE defaults to http://localhost:5173)
//
// Covers: 14231 order 5-state flow, 14232 customer cancel, 14234 favorite,
//         14235 cart, 14236 cart checkout, 14237 cart-to-favorite.
const { chromium } = require("playwright");
const assert = require("node:assert/strict");

(async () => {
  const browser = await chromium.launch({ headless: true });
  const base = process.env.E2E_BASE || "http://localhost:5173";
  try {
    // 两个独立上下文：卖家与客户各自一个会话，互不干扰
    const sellerCtx = await browser.newContext();
    const customerCtx = await browser.newContext();
    const seller = await sellerCtx.newPage();
    const customer = await customerCtx.newPage();
    seller.setDefaultTimeout(10000);
    customer.setDefaultTimeout(10000);
    const errors = [];
    for (const p of [seller, customer]) p.on("pageerror", (e) => errors.push(e.message));

    // 1. 卖家登录 + 发布商品
    await seller.goto(`${base}/seller/login`);
    await seller.getByLabel("用户名", { exact: true }).fill("admin");
    await seller.getByLabel("密码", { exact: true }).fill("123456");
    await seller.getByRole("button", { name: "登录工作台" }).click();
    await seller.waitForURL("**/seller/admin");

    await seller.goto(`${base}/seller/publish`);
    await seller.getByLabel("商品名称", { exact: true }).fill("E2E B 台灯");
    await seller.getByRole("spinbutton").first().fill("35");
    await seller.getByRole("spinbutton").nth(1).fill("5");
    await seller.getByRole("button", { name: "确认发布" }).click();
    await seller.waitForURL("**/seller/products");

    // 2. 客户注册（自动登录）
    await customer.goto(`${base}/customer/register`);
    await customer.getByLabel("用户名", { exact: true }).fill("e2e李四");
    await customer.getByLabel("密码", { exact: true }).fill("abc12345");
    await customer.getByLabel("确认密码", { exact: true }).fill("abc12345");
    await customer.getByLabel("联系电话", { exact: true }).fill("13800001111");
    await customer.getByLabel("默认交易地点", { exact: true }).fill("图书馆门口");
    await customer.locator("form").getByRole("button", { name: "注册" }).click();
    await customer.waitForURL("**/customer/orders");

    // 3. 商品详情：收藏 + 加入购物车
    await customer.goto(`${base}/`);
    await customer.getByText("E2E B 台灯", { exact: true }).click();
    await customer.waitForURL("**/product/**");
    const productId = Number(customer.url().split("/").pop());
    assert.ok(Number.isInteger(productId) && productId > 0);
    await customer.getByRole("button", { name: "收藏" }).click();
    await customer.getByRole("button", { name: "加入购物车" }).click();

    // 4. 收藏页能看到商品
    await customer.goto(`${base}/customer/favorites`);
    await customer.getByText("E2E B 台灯", { exact: true }).waitFor();

    // 5. 购物车：勾选后去下单
    await customer.goto(`${base}/customer/cart`);
    await customer.getByText("E2E B 台灯", { exact: true }).waitFor();
    await customer.locator(".el-table__row").first().locator(".el-checkbox").click();
    await customer.getByRole("button", { name: "去下单" }).click();
    await customer.waitForURL("**/customer/orders");

    // 6. 我的订单：下单成功，处于「客户下单」，可取消
    await customer.locator(".el-tag").filter({ hasText: "客户下单" }).first().waitFor();
    await customer.getByRole("button", { name: "取消订单" }).waitFor();

    // 7. 卖家订单管理：看到客户信息，五状态逐步推进
    await seller.goto(`${base}/seller/orders`);
    await seller.getByText("e2e李四", { exact: true }).waitFor();
    await seller.locator(".el-tag").filter({ hasText: "客户下单" }).first().waitFor();
    await seller.getByRole("button", { name: "确认" }).click();
    await seller.getByRole("button", { name: "备货完成" }).waitFor();
    await seller.getByRole("button", { name: "备货完成" }).click();
    await seller.getByRole("button", { name: "开始发货" }).waitFor();
    await seller.getByRole("button", { name: "开始发货" }).click();
    await seller.getByRole("button", { name: "交易完成" }).waitFor();
    await seller.getByRole("button", { name: "交易完成" }).click();
    await seller.locator(".el-tag").filter({ hasText: "交易完成" }).first().waitFor();
    assert.equal(await seller.getByRole("button", { name: "取消" }).count(), 0);

    // 8. 客户刷新我的订单：已交易完成，不能再取消
    await customer.goto(`${base}/customer/orders`);
    await customer.locator(".el-tag").filter({ hasText: "交易完成" }).first().waitFor();
    assert.equal(await customer.getByRole("button", { name: "取消订单" }).count(), 0);

    // 9. 客户取消：再下一单，未发货时取消
    await customer.goto(`${base}/product/${productId}`);
    await customer.getByRole("button", { name: "加入购物车" }).click();
    await customer.goto(`${base}/customer/cart`);
    await customer.locator(".el-table__row").first().locator(".el-checkbox").click();
    await customer.getByRole("button", { name: "去下单" }).click();
    await customer.waitForURL("**/customer/orders");
    await customer.getByRole("button", { name: "取消订单" }).first().click();
    await customer.locator(".el-tag").filter({ hasText: "已取消" }).first().waitFor();

    // 10. 购物车多选转收藏：加购后转收藏，购物车清空、收藏仍在
    await customer.goto(`${base}/product/${productId}`);
    await customer.getByRole("button", { name: "加入购物车" }).click();
    await customer.goto(`${base}/customer/cart`);
    await customer.getByText("E2E B 台灯", { exact: true }).waitFor();
    await customer.locator(".el-table__row").first().locator(".el-checkbox").click();
    await customer.getByRole("button", { name: "转收藏" }).click();
    await customer.getByText("购物车还是空的").waitFor();
    await customer.goto(`${base}/customer/favorites`);
    await customer.getByText("E2E B 台灯", { exact: true }).waitFor();

    assert.deepEqual(errors, []);
    console.log(
      "PASS: publish -> favorite -> cart -> checkout -> 5-state order flow -> customer cancel -> cart-to-favorite; no page errors",
    );
    await sellerCtx.close();
    await customerCtx.close();
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
