<template>
  <div class="query-container">
    <el-card>
      <template #header><h2 style="margin: 0">口令查询</h2></template>

      <el-form inline @submit.prevent>
        <el-form-item>
          <el-input
            v-model="code"
            placeholder="请输入购买意向的口令码"
            clearable
            style="width: 260px"
            @keyup.enter="query"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="query">查询</el-button>
        </el-form-item>
      </el-form>

      <el-empty
        v-if="!result"
        description="输入口令码后查询排队位次与交易状态"
      />

      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="排队位次">{{ result.position }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ result.statusText }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ result.productName }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ result.name }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ result.phone }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ result.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getIntentByCode } from '@/api/buyer'

const route = useRoute()
const code = ref(route.query.code || '')
const loading = ref(false)
const result = ref(null)

async function query() {
  const trimmed = code.value.trim()
  if (!trimmed) {
    ElMessage.warning('请输入口令码')
    return
  }
  loading.value = true
  try {
    result.value = await getIntentByCode(trimmed)
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.query-container {
  max-width: 600px;
  margin: 60px auto;
}
</style>