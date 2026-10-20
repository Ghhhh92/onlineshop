// Real end-to-end regression: no mock, hits a real Spring Boot backend through the
// Vite dev proxy (/api -> 127.0.0.1:8000). Assumes a freshly started backend (empty DB),
// otherwise the single-on-sale constraint would reject the publish step.
//   Usage: node tests/intent-flow-e2e.cjs   (E2E_BASE defaults to http://localhost:5173)
const { chromium } = require("playwright");
const assert = require("node:assert/strict");
(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    page.setDefaultTimeout(8000);
    const failures = [];
    page.on("pageerror", (e) => failures.push(e.message));
    const base = process.env.E2E_BASE || "http://localhost:5173";

    // 1. Buyer home starts empty (verifies /api/product/on-sale empty state)
    await page.goto(`${base}/`);
    await page.getByText("暂无在售商品").waitFor();

    // 2. Seller logs in
    await page.goto(`${base}/seller/login`);
    await page.getByLabel("用户名", { exact: true }).fill("admin");
    await page.getByLabel("密码", { exact: true }).fill("123456");
    await page.getByRole("button", { name: "登录工作台" }).click();
    await page.waitForURL("**/seller/admin");

    // 3. Seller publishes a product
    await page.goto(`${base}/seller/publish`);
    await page.getByLabel("商品名称", { exact: true }).fill("E2E 回归商品");
    await page.getByRole("spinbutton").fill("12.34");
    await page.getByRole("spinbutton").press("Tab");
    await page.getByRole("button", { name: "确认发布" }).click();
    await page.waitForURL("**/seller/admin");
    await page.getByRole("heading", { name: "E2E 回归商品", exact: true }).waitFor();
    await page.getByText("在售", { exact: true }).first().waitFor();

    // 4. Buyer submits intent and captures the 口令码
    await page.goto(`${base}/`);
    await page.getByRole("heading", { name: "E2E 回归商品", exact: true }).waitFor();
    await page.getByRole("button", { name: "我要购买" }).click();
    await page.waitForURL("**/buy");
    await page.getByLabel("姓名", { exact: true }).fill("测试买家");
    await page.getByLabel("电话", { exact: true }).fill("13800001234");
    await page.getByRole("button", { name: "提交" }).click();
    await page.waitForURL("**/done/**");
    const code = (await page.locator(".code").innerText()).trim();
    assert.match(code, /^[A-HJ-NP-Z2-9]{8}$/);
    await page.getByText("当前排第 1 位").waitFor();

    // 5. Buyer queries by 口令码 (real code, masked phone)
    await page.goto(`${base}/query?code=${code}`);
    await page.getByRole("button", { name: "查询" }).click();
    await page.getByText("排队中", { exact: true }).waitFor();
    await page.getByText("E2E 回归商品", { exact: true }).waitFor();
    await page.getByText("138****1234", { exact: true }).waitFor();

    // 6. Seller sees the buyer in the intent list (full phone)
    await page.goto(`${base}/seller/admin`);
    await page.getByRole("button", { name: "刷新", exact: true }).click();
    await page.getByText("测试买家", { exact: true }).waitFor();
    await page.getByText("13800001234", { exact: true }).waitFor();

    assert.deepEqual(failures, []);
    console.log(
      `PASS: empty home -> seller login -> publish -> buyer submit (code=${code}) -> query by code -> seller intent list; no page errors`,
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
