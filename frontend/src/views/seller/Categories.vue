<template><SellerLayout>
<div class="page-heading"><h1>类别管理</h1><div><el-button @click="load">刷新</el-button><el-button type="primary" @click="edit(null,0)">新增一级类别</el-button></div></div>
<section class="panel" v-loading="loading"><el-alert v-if="error" :title="error" type="error" :closable="false"/>
<el-table v-else :data="categories" row-key="id" default-expand-all empty-text="暂无类别"><el-table-column prop="name" label="类别名称" min-width="180"/><el-table-column prop="sort" label="排序" width="90"/><el-table-column label="操作" min-width="250"><template #default="{row}"><el-button v-if="categories.some(c=>c.id===row.id)" link @click="edit(null,row.id)">新增二级类别</el-button><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" :disabled="busy" @click="remove(row)">删除</el-button></template></el-table-column></el-table>
</section>
<el-dialog v-model="visible" :title="form.id?'编辑类别':'新增类别'" width="min(440px, 92vw)" :close-on-click-modal="false"><el-form label-position="top" @submit.prevent="save"><el-form-item label="名称"><el-input v-model="form.name" maxlength="100"/></el-form-item><el-form-item label="排序（小的在前）"><el-input-number v-model="form.sort" :precision="0" :min="0" :max="2147483647"/></el-form-item><el-button type="primary" native-type="submit" :loading="busy">保存类别</el-button></el-form></el-dialog>
</SellerLayout></template>
<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import SellerLayout from '@/components/SellerLayout.vue';
import { getCategories, saveCategory, deleteCategory } from '@/api/sellerWorkspace';
const categories=ref([]),loading=ref(false),error=ref(''),visible=ref(false),busy=ref(false),form=reactive({id:null,name:'',parentId:0,sort:0});
async function load(){loading.value=true;error.value='';try{categories.value=await getCategories();}catch{error.value='类别加载失败，请刷新重试';}finally{loading.value=false;}}
function edit(row,parentId=0){Object.assign(form,{id:row?.id||null,name:row?.name||'',parentId,sort:row?.sort||0});visible.value=true;}
async function save(){if(busy.value)return;if(!form.name.trim()){ElMessage.warning('请输入类别名称');return;}busy.value=true;try{await saveCategory(form.id,{name:form.name.trim(),parentId:form.parentId,sort:form.sort??0});visible.value=false;ElMessage.success('类别已保存');await load();}catch{}finally{busy.value=false;}}
async function remove(row){try{await ElMessageBox.confirm(`确定删除“${row.name}”？有子类别或商品的类别不能删除。`,'删除类别',{type:'warning'});}catch{return;}busy.value=true;try{await deleteCategory(row.id);ElMessage.success('类别已删除');await load();}catch{}finally{busy.value=false;}}
onMounted(load);
</script>
