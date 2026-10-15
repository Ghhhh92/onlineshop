// Isolated UI tests: mock HTTP responses; no database or real seller data is changed.
// Install Playwright separately, then run: node tests/seller-smoke.cjs
const { chromium } = require("playwright");
const assert = require("node:assert/strict");
(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    page.setDefaultTimeout(8000);
    const failures = [];
    page.on("pageerror", (e) => failures.push(e.message));
    let authenticated = false,
      current = null,
      unavailable = false,
      published = 0,
      passwordChanges = 0;
    const sample = {
      id: 1,
      name: "测试台灯",
      price: 35.5,
      status: "FROZEN",
      createTime: "2026-10-14 10:00:00",
    };
    await page.route("**/api/**", async (route) => {
      const req = route.request(),
        url = new URL(req.url()),
        path = url.pathname;
      if (!path.startsWith("/api/")) return route.continue();
      const reply = (data, status = 200, message = "") =>
        route.fulfill({ status, json: { code: status, data, message } });
      if (path === "/api/seller/me")
        return authenticated
          ? reply({ username: "admin", id: 1 })
          : reply(null, 401, "请先登录");
      if (path === "/api/seller/login") {
        if (req.postDataJSON().password !== "correctpass")
          return reply(null, 400, "用户名或密码错误");
        authenticated = true;
        return reply({ username: "admin", id: 1 });
      }
      if (path === "/api/seller/logout") {
        authenticated = false;
        return reply(null);
      }
      if (path === "/api/seller/password") {
        passwordChanges++;
        authenticated = false;
        return reply(null);
      }
      if (unavailable) return reply(null, 404);
      if (
        path === "/api/seller/product/current" ||
        path === "/api/product/on-sale"
      )
        return reply(current);
      if (path === "/api/product/publish") {
        published++;
        current = { ...sample, ...req.postDataJSON(), status: "ON_SALE" };
        return reply(current);
      }
      if (path.endsWith("/intents"))
        return reply([
          {
            id: 2,
            name: "后提交",
            phone: "13800000002",
            createTime: "2026-10-14 11:00:00",
            status: "FAILED",
            position: 2,
          },
          {
            id: 1,
            name: "先提交",
            phone: "13800000001",
            createTime: "2026-10-14 10:00:00",
            status: "SUCCESS",
            position: 1,
          },
        ]);
      if (path === "/api/seller/product/history")
        return reply({
          records: [
            {
              ...sample,
              name: `历史商品第${url.searchParams.get("page")}页`,
              status: "DELISTED",
              finishTime: "2026-10-15 10:00:00",
            },
          ],
          total: 11,
        });
      return reply(null, 404);
    });
    const base = process.env.SELLER_TEST_URL || "http://127.0.0.1:5173";
    await page.goto(`${base}/seller/admin`);
    await page.waitForURL("**/seller/login");
    await page.getByRole("button", { name: "登录工作台" }).click();
    await page.getByText("请输入用户名", { exact: true }).waitFor();
    await page.getByLabel("用户名", { exact: true }).fill("admin");
    await page.getByLabel("密码", { exact: true }).fill("wrong");
    await page.getByRole("button", { name: "登录工作台" }).click();
    await page.getByText("用户名或密码错误").first().waitFor();
    await page.getByLabel("密码", { exact: true }).fill("correctpass");
    await page.getByRole("button", { name: "登录工作台" }).click();
    await page.waitForURL("**/seller/admin");
    await page.reload();
    await page.getByRole("heading", { name: "后台首页" }).waitFor();
    current = sample;
    await page.getByRole("button", { name: "刷新", exact: true }).click();
    await page.getByText("已冻结 · 交易中").waitFor();
    await page.getByText("先提交", { exact: true }).waitFor();
    assert.match(
      await page.locator(".el-table__body-wrapper").innerText(),
      /先提交[\s\S]*后提交/,
    );
    await page.getByRole("link", { name: "发布商品", exact: true }).click();
    await page
      .getByText("已有在售或冻结商品，请先处理当前商品后再发布。")
      .waitFor();
    assert.equal(
      await page.getByRole("button", { name: "确认发布" }).isDisabled(),
      true,
    );
    current = null;
    await page.reload();
    await page.getByRole("button", { name: "确认发布" }).waitFor();
    await page.getByLabel("商品名称", { exact: true }).fill("  新商品  ");
    await page.getByRole("spinbutton").fill("12.34");
    await page.getByRole("spinbutton").press("Tab");
    await page.getByRole("button", { name: "确认发布" }).click();
    await page.waitForURL("**/seller/admin");
    await page.getByRole("heading", { name: "新商品", exact: true }).waitFor();
    assert.equal(published, 1);
    await page.getByRole("link", { name: "历史商品", exact: true }).click();
    await page.getByText("历史商品第1页", { exact: true }).waitFor();
    await page.getByRole("button", { name: "Go to next page" }).click();
    await page.getByText("历史商品第2页", { exact: true }).waitFor();
    await page.getByRole("button", { name: "查看明细" }).click();
    await page.getByRole("heading", { name: "意向人及交易结果" }).waitFor();
    await page
      .getByRole("dialog")
      .getByText("交易失败", { exact: true })
      .waitFor();
    await page.getByRole("button", { name: "Close this dialog" }).click();
    unavailable = true;
    await page.getByRole("button", { name: "刷新", exact: true }).click();
    await page
      .getByText("历史查询暂不可用，等待服务接通后即可查看历史记录。")
      .waitFor();
    unavailable = false;
    await page.getByRole("button", { name: "修改密码", exact: true }).click();
    await page.getByLabel("原密码", { exact: true }).fill("correctpass");
    await page.getByLabel("新密码", { exact: true }).fill("newpassword");
    await page.getByLabel("确认新密码", { exact: true }).fill("newpassword");
    await page.getByRole("button", { name: "保存并重新登录" }).click();
    await page.waitForURL("**/seller/login");
    assert.equal(passwordChanges, 1);
    authenticated = true;
    await page.goto(`${base}/seller/admin`);
    await page.getByRole("button", { name: "退出登录", exact: true }).click();
    await page.waitForURL("**/seller/login");
    authenticated = true;
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(`${base}/seller/admin`);
    await page.getByRole("heading", { name: "后台首页" }).waitFor();
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      true,
    );
    await page.screenshot({
      path: "/private/tmp/onlineshop-seller-mobile.png",
      fullPage: true,
    });
    assert.deepEqual(failures, []);
    console.log(
      "PASS: auth, login errors, session restoration, frozen constraint, publish, intent ordering, history pagination/detail, unavailable endpoint, password, logout, mobile, no page errors",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
