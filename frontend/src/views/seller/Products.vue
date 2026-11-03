<template>
<SellerLayout>
  <div class="page-heading"><h1>商品列表与库存</h1><el-button @click="load">刷新</el-button></div>
  <section class="panel">
    <el-form inline @submit.prevent="search">
      <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width:150px"><el-option v-for="(label,key) in states" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="类别"><el-select v-model="filters.categoryId" clearable placeholder="全部类别" style="width:200px"><el-option-group v-for="root in categories" :key="root.id" :label="root.name"><el-option v-for="child in root.children" :key="child.id" :label="child.name" :value="child.id" /></el-option-group></el-select></el-form-item>
      <el-button native-type="submit" type="primary">查询</el-button>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table v-else :data="rows" v-loading="loading" empty-text="暂无商品">
      <el-table-column prop="name" label="商品" min-width="170" />
      <el-table-column label="类别" min-width="130"><template #default="{row}">{{ categoryName(row.categoryId) }}</template></el-table-column>
      <el-table-column prop="price" label="价格（元）" width="120" />
      <el-table-column label="状态" width="100"><template #default="{row}">{{ row.statusText || states[row.status] }}</template></el-table-column>
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="createTime" label="发布时间" min-width="180" />
      <el-table-column label="操作" width="200"><template #default="{row}"><el-button link type="primary" :disabled="row.status === 'DELISTED'" @click="editStock(row)">维护库存</el-button><el-button link @click="showIntents(row)">意向人</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next, total" @current-change="load" />
  </section>
  <el-dialog v-model="stockVisible" title="维护库存" width="min(440px, 92vw)" :close-on-click-modal="false">
    <p>{{ selected?.name }} · 当前库存 {{ selected?.stock }}</p><p class="muted">填写调整后的库存总数（1～99999），售罄商品补货后恢复在售。</p>
    <el-input-number v-model="stock" :min="1" :max="99999" :precision="0" aria-label="库存总数" />
    <template #footer><el-button @click="stockVisible=false" :disabled="saving">取消</el-button><el-button type="primary" :loading="saving" @click="saveStock">保存库存</el-button></template>
  </el-dialog>
  <el-dialog v-model="intentVisible" title="商品意向人" width="min(960px, 94vw)"><el-alert v-if="intentError" :title="intentError" type="error"/><div v-else v-loading="intentLoading"><IntentTable :items="intents" /></div></el-dialog>
</SellerLayout>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import SellerLayout from '@/components/SellerLayout.vue';
import IntentTable from '@/components/IntentTable.vue';
import { getProducts, getCategories, updateStock, getProductIntents } from '@/api/sellerWorkspace';
const states={ON_SALE:'在售',FROZEN:'已冻结',SALES_OUT:'售罄',DELISTED:'已下架'};
const filters=reactive({status:'',categoryId:undefined});
const rows=ref([]),categories=ref([]),page=ref(1),total=ref(0),loading=ref(false),error=ref('');
const stockVisible=ref(false),selected=ref(null),stock=ref(1),saving=ref(false),intentVisible=ref(false),intents=ref([]),intentError=ref(''),intentLoading=ref(false);
let request=0;
async function load(){const token=++request; loading.value=true;error.value='';try{const data=await getProducts({page:page.value,size:10,status:filters.status||undefined,categoryId:filters.categoryId||undefined});if(token===request){rows.value=data.records;total.value=data.total;}}catch{if(token===request)error.value='商品加载失败，请刷新重试';}finally{if(token===request)loading.value=false;}}
function search(){page.value=1;load();}
function categoryName(id){for(const root of categories.value){const child=root.children.find(c=>c.id===id);if(child)return `${root.name} / ${child.name}`;}return id?'类别不可用':'未分类';}
function editStock(row){selected.value=row;stock.value=Math.max(1,row.stock||0);stockVisible.value=true;}
async function saveStock(){if(saving.value||!Number.isInteger(stock.value)||stock.value<1||stock.value>99999)return; saving.value=true;try{await updateStock(selected.value.id,stock.value);stockVisible.value=false;ElMessage.success('库存已更新');await load();}catch{}finally{saving.value=false;}}
async function showIntents(row){intentVisible.value=true;intentError.value='';intents.value=[];intentLoading.value=true;try{intents.value=await getProductIntents(row.id);}catch{intentError.value='意向人加载失败，请关闭后重试';}finally{intentLoading.value=false;}}
onMounted(async()=>{load();try{categories.value=await getCategories();}catch{ElMessage.error('类别加载失败，请刷新页面重试');}});
</script>
