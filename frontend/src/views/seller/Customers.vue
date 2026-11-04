<template><SellerLayout>
<div class="page-heading"><h1>客户列表</h1><el-button @click="load">刷新</el-button></div>
<section class="panel"><el-alert v-if="error" :title="error" type="error" :closable="false"/><el-table v-else :data="rows" v-loading="loading" empty-text="暂无注册客户"><el-table-column prop="username" label="用户名" min-width="130"/><el-table-column prop="phone" label="电话" min-width="140"/><el-table-column prop="defaultLocation" label="默认交易地点" min-width="180"/><el-table-column prop="createTime" label="注册时间" min-width="180"/><el-table-column label="操作" width="130"><template #default="{row}"><el-button link type="primary" @click="open(row)">购买历史</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="prev, pager, next, total" @current-change="load"/></section>
<el-dialog v-model="visible" :title="`${customer?.username||''} · 购买历史`" width="min(960px, 94vw)"><el-alert v-if="orderError" :title="orderError" type="error"><el-button @click="loadOrders">重试</el-button></el-alert><el-table v-else :data="orders" v-loading="orderLoading" empty-text="暂无购买记录"><el-table-column label="商品" min-width="150"><template #default="{row}">{{ row.productName || '商品已不存在' }}</template></el-table-column><el-table-column prop="price" label="价格（元）" width="120"/><el-table-column prop="createTime" label="下单时间" min-width="180"/><el-table-column label="交易结果" width="120"><template #default="{row}">{{ row.statusText || statuses[row.status] || '—' }}</template></el-table-column></el-table><el-pagination v-model:current-page="orderPage" :total="orderTotal" :page-size="10" layout="prev, pager, next, total" @current-change="loadOrders"/></el-dialog>
</SellerLayout></template>
<script setup>
import { ref, onMounted } from 'vue';
import SellerLayout from '@/components/SellerLayout.vue';
import { getCustomers, getCustomerOrders } from '@/api/sellerWorkspace';
const rows=ref([]),page=ref(1),total=ref(0),loading=ref(false),error=ref(''),visible=ref(false),customer=ref(null),orders=ref([]),orderPage=ref(1),orderTotal=ref(0),orderError=ref(''),orderLoading=ref(false);
const statuses={SUCCESS:'成功',FAILED:'失败',CANCELED:'已撤销',QUEUING:'排队中',TRADING:'交易中'};
let listRequest=0,orderRequest=0;
async function load(){const token=++listRequest;loading.value=true;error.value='';try{const data=await getCustomers({page:page.value,size:10});if(token===listRequest){rows.value=data.records;total.value=data.total;}}catch{if(token===listRequest)error.value='客户加载失败，请刷新重试';}finally{if(token===listRequest)loading.value=false;}}
function open(row){customer.value=row;orderPage.value=1;orders.value=[];orderTotal.value=0;visible.value=true;loadOrders();}
async function loadOrders(){const token=++orderRequest;orderLoading.value=true;orderError.value='';try{const data=await getCustomerOrders(customer.value.id,{page:orderPage.value,size:10});if(token===orderRequest){orders.value=data.records;orderTotal.value=data.total;}}catch{if(token===orderRequest)orderError.value='购买历史加载失败';}finally{if(token===orderRequest)orderLoading.value=false;}}
onMounted(load);
</script>
