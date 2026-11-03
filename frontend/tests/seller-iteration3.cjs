const { chromium } = require('playwright');
const assert = require('node:assert/strict');
(async()=>{
 const base=process.env.SELLER_TEST_URL || 'http://127.0.0.1:5183';
 const browser=await chromium.launch({headless:true, executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH});
 try {
 const page=await browser.newPage();page.setDefaultTimeout(8000);const errors=[];page.on('pageerror',e=>errors.push(e.message));
 let uploads=0;
 let stock=0, saved=false, deleted=false, published;
 await page.route('**/api/**',async route=>{
 const req=route.request(),url=new URL(req.url()),p=url.pathname;
 if(!p.startsWith('/api/'))return route.continue();
 const ok=data=>route.fulfill({json:{code:200,data,message:'success'}});
 if(p==='/api/seller/me')return ok({id:1,username:'admin'});
 if(p==='/api/category/tree')return ok([{id:1,name:'家居',sort:0,children:deleted?[]:[{id:2,name:'灯具',sort:1,children:[]}]}]);
 if(p==='/api/seller/product/list')return ok({records:[{id:1,name:'测试台灯',price:35,stock,status:stock?'ON_SALE':'SALES_OUT',statusText:stock?'在售':'售罄',categoryId:2}],total:11});
 if(p.endsWith('/stock')){stock=req.postDataJSON().stock;return ok({});}
 if(p==='/api/seller/category' || p==='/api/seller/category/2'){if(req.method()==='DELETE')deleted=true;else saved=true;return ok({});}
 if(p==='/api/seller/customer/list')return ok({records:[{id:1,username:'客户甲',phone:'13800000000',defaultLocation:'学校',createTime:'2026-11-03'}],total:1});
 if(p.endsWith('/orders'))return ok({records:[{intentId:1,productName:'测试台灯',price:35,statusText:'失败',createTime:'2026-11-03'}],total:1});
 if(p==='/api/product/image')return ok({url:`/uploads/test-${++uploads}.png`});
 if(p==='/api/product/publish'){published=req.postDataJSON();return ok({id:3});}
 return ok([]);
 });
 await page.goto(`${base}/seller/admin`);
 await page.getByText('测试台灯',{exact:true}).waitFor();
 await page.getByText('维护库存',{exact:true}).click();
 await page.getByRole('spinbutton').fill('5');await page.getByRole('button',{name:'保存库存'}).click();
 await page.locator('.el-table').getByText('在售',{exact:true}).waitFor();assert.equal(stock,5);
 await page.getByRole('link',{name:'类别管理',exact:true}).click();
 await page.getByRole('button',{name:'新增一级类别'}).click();await page.locator('.el-dialog input').first().fill('新类别');await page.getByRole('button',{name:'保存类别'}).click();await page.waitForTimeout(200);assert(saved);
 await page.getByRole('button',{name:'删除',exact:true}).last().click();await page.locator('.el-message-box .el-button--primary').click();await page.waitForTimeout(200);assert(deleted);
 await page.getByRole('link',{name:'客户列表',exact:true}).click();await page.getByText('客户甲',{exact:true}).waitFor();await page.getByRole('button',{name:'购买历史'}).click();await page.getByText('失败',{exact:true}).waitFor();
 await page.goto(`${base}/seller/publish`);await page.getByPlaceholder('例如：九成新台灯').fill('新增商品');await page.getByRole('spinbutton').first().fill('12');await page.getByLabel('上传商品图片').setInputFiles([{name:'one.png',mimeType:'image/png',buffer:Buffer.from('test')},{name:'two.png',mimeType:'image/png',buffer:Buffer.from('test')}]);await page.getByRole('button',{name:'前移'}).last().waitFor();await page.getByRole('button',{name:'前移'}).last().click();await page.getByRole('button',{name:'确认发布'}).click();await page.waitForURL('**/seller/products');assert.equal(published.stock,1);assert.deepEqual(published.images,['/uploads/test-2.png','/uploads/test-1.png']);
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:process.env.SELLER_TEST_SCREENSHOT || '/tmp/seller-iteration3-mobile.png',fullPage:true});
 assert.deepEqual(errors,[]);console.log('PASS: stock, categories, customer orders, multi-product publish, mobile render');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
