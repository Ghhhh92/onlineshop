// Isolated acceptance checks for US-38; no business data is written.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless:true, executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH});
  try {
    const page = await browser.newPage();
    page.setDefaultTimeout(8000);
    let empty = false, unavailable = false;
    const errors=[];
    page.on('pageerror', error=>errors.push(error.message));
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      if (!path.startsWith('/api/')) return route.continue();
      const reply = data => route.fulfill({json:{code:200,data}});
      if(path==='/api/seller/me')return reply({id:1,username:'admin'});
      if(path==='/api/seller/customer/list')return reply({records:empty?[]:[{id:7,username:'客户甲',phone:'13800000000',defaultLocation:'校园东门',createTime:'2026-11-04 10:00:00'}],total:empty?0:1});
      if(path==='/api/seller/customer/7/orders'){
        if(unavailable)return route.fulfill({status:500,json:{message:'查询失败'}});
        return reply({records:[{intentId:1,productName:'台灯',createTime:'2026-11-04 11:00:00',status:'SUCCESS'},{intentId:2,productName:'键盘',createTime:'2026-11-03 11:00:00',status:'FAILED'}],total:2});
      }
      return reply([]);
    });
    await page.goto(`${process.env.SELLER_TEST_URL || 'http://127.0.0.1:5173'}/seller/customers`);
    for(const text of ['客户甲','13800000000','校园东门','2026-11-04 10:00:00'])await page.getByText(text,{exact:true}).waitFor();
    await page.getByRole('button',{name:'购买历史'}).click();
    for(const text of ['台灯','键盘','成功','失败','2026-11-04 11:00:00'])await page.getByText(text,{exact:true}).waitFor();
    await page.locator('.el-dialog__headerbtn').click();
    unavailable=true;
    await page.getByRole('button',{name:'购买历史'}).click();
    await page.getByText('购买历史加载失败',{exact:true}).waitFor();
    unavailable=false;
    await page.getByRole('button',{name:'重试',exact:true}).click();
    await page.getByText('键盘',{exact:true}).waitFor();
    await page.locator('.el-dialog__headerbtn').click();
    empty=true;
    await page.getByRole('button',{name:'刷新',exact:true}).click();
    await page.getByText('暂无注册客户',{exact:true}).waitFor();
    assert.equal(await page.getByRole('button',{name:'购买历史'}).count(),0);
    assert.deepEqual(errors,[]);
    console.log('PASS: customer fields, purchase history, success/failed results, retry, empty customer list');
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exit(1)});
