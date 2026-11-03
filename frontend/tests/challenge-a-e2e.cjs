// Real end-to-end regression for Challenge A buyer features (no mock): hits a real
// Spring Boot backend through the Vite dev proxy (/api -> 127.0.0.1:8000).
// Assumes a freshly started backend (empty DB) and Vite dev server on localhost:5173.
//   Usage: node tests/challenge-a-e2e.cjs   (E2E_BASE defaults to http://localhost:5173)
//
// Covers: US-31 multi-product, US-34 search, US-35 multi-image + rich description,
//         US-36 customer register, US-37 my orders.
const { chromium } = require("playwright");
const assert = require("node:assert/strict");

// 1x1 transparent PNG (valid magic bytes; backend recognises PNG by content)
const PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=",
  "base64",
);

(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    page.setDefaultTimeout(10000);
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    const base = process.env.E2E_BASE || "http://localhost:5173";

    // 1. Seller login
    await page.goto(`${base}/seller/login`);
    await page.getByLabel("用户名", { exact: true }).fill("admin");
    await page.getByLabel("密码", { exact: true }).fill("123456");
    await page.getByRole("button", { name: "登录工作台" }).click();
    await page.waitForURL("**/seller/admin");

    // 2. Publish product A (two images + description)  ->  US-35
    await page.goto(`${base}/seller/publish`);
    await page.getByLabel("商品名称", { exact: true }).fill("E2E 台灯");
    await page.getByPlaceholder("介绍成色、规格及交易方式").fill("九成新台灯，功能完好");
    await page.getByLabel("上传商品图片").setInputFiles([
      { name: "one.png", mimeType: "image/png", buffer: PNG },
      { name: "two.png", mimeType: "image/png", buffer: PNG },
    ]);
    await page.getByRole("button", { name: "前移" }).first().waitFor(); // both images uploaded
    await page.getByRole("spinbutton").first().fill("35");
    await page.getByRole("spinbutton").nth(1).fill("5");
    await page.getByRole("button", { name: "确认发布" }).click();
    await page.waitForURL("**/seller/products");

    // 3. Publish product B (no image)  ->  US-31 multi-product
    await page.goto(`${base}/seller/publish`);
    await page.getByLabel("商品名称", { exact: true }).fill("E2E 小说");
    await page.getByRole("spinbutton").first().fill("12.5");
    await page.getByRole("spinbutton").nth(1).fill("3");
    await page.getByRole("button", { name: "确认发布" }).click();
    await page.waitForURL("**/seller/products");

    // 4. Buyer home shows both products  ->  US-31
    await page.goto(`${base}/`);
    await page.getByText("E2E 台灯", { exact: true }).waitFor();
    await page.getByText("E2E 小说", { exact: true }).waitFor();

    // 5. Keyword search filters the list  ->  US-34
    await page.getByPlaceholder("搜索商品名称或描述").fill("台灯");
    await page.getByRole("button", { name: "搜索" }).click();
    await page.getByText("E2E 台灯", { exact: true }).waitFor();
    assert.equal(await page.getByText("E2E 小说", { exact: true }).count(), 0);

    // 6. Detail: multi-image carousel + description  ->  US-35
    await page.getByText("E2E 台灯", { exact: true }).click();
    await page.waitForURL("**/product/**");
    const productId = Number(page.url().split("/").pop());
    assert.ok(Number.isInteger(productId) && productId > 0);
    await page.locator(".el-carousel__item").nth(1).waitFor({ state: "attached" });
    const srcs = await page
      .locator(".el-carousel__item img")
      .evaluateAll((imgs) => [...new Set(imgs.map((i) => i.getAttribute("src")))]);
    assert.equal(srcs.length, 2); // 多图轮播：两张不同的图片
    await page.getByText("九成新台灯，功能完好").waitFor();
    await page.getByRole("button", { name: "我要购买" }).waitFor();

    // 7. Customer register (auto login)  ->  US-36
    await page.goto(`${base}/customer/register`);
    await page.getByLabel("用户名", { exact: true }).fill("e2e张三");
    await page.getByLabel("密码", { exact: true }).fill("abc12345");
    await page.getByLabel("确认密码", { exact: true }).fill("abc12345");
    await page.getByLabel("联系电话", { exact: true }).fill("13800001234");
    await page.getByLabel("默认交易地点", { exact: true }).fill("图书馆门口");
    await page.locator("form").getByRole("button", { name: "注册" }).click();
    await page.waitForURL("**/customer/orders");
    await page.getByText("还没有下单记录").waitFor();

    // 8. Submit intent while logged in
    await page.goto(`${base}/product/${productId}`);
    await page.getByRole("button", { name: "我要购买" }).click();
    await page.waitForURL("**/buy**");
    await page.getByLabel("姓名", { exact: true }).fill("张三");
    await page.getByLabel("联系电话", { exact: true }).fill("13800001234");
    await page.getByRole("button", { name: "提交购买意向" }).click();
    await page.waitForURL("**/done/**");
    const code = (await page.locator(".code").innerText()).trim();
    assert.match(code, /^[A-HJ-NP-Z2-9]{8}$/);
    await page.getByText("当前排第 1 位").waitFor();

    // 9. My orders shows the order  ->  US-37
    await page.goto(`${base}/customer/orders`);
    await page.getByText("E2E 台灯", { exact: true }).waitFor();
    await page.getByText("排队中", { exact: true }).waitFor();

    assert.deepEqual(errors, []);
    console.log(
      `PASS: multi-product -> search -> multi-image detail -> register -> submit intent (code=${code}) -> my orders; no page errors`,
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
