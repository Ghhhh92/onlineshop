<template>
  <div class="publish-container">
    <el-card>
      <template #header><h2 style="margin: 0">发布商品</h2></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述（选填）" />
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-input v-model="form.image" placeholder="请输入图片 URL（选填）" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { publishProduct } from '@/api/product'

const formRef = ref(null)
const loading = ref(false)

const form = reactive({ name: '', description: '', image: '', price: null })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await publishProduct(form)
    ElMessage.success('发布成功')
    form.name = ''
    form.description = ''
    form.image = ''
    form.price = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.publish-container {
  padding: 40px;
}
</style>
