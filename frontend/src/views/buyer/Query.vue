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

      <!-- 待后端「意向/位次查询」接口就绪后填充查询结果，字段名届时与后端对齐 -->
      <el-empty
        v-if="!result"
        description="输入口令码后查询排队位次与交易状态（待后端接口对接）"
      />

      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="排队位次">{{ result.position }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ result.statusText }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ result.name }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ result.phone }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const code = ref(route.query.code || '')
const loading = ref(false)
const result = ref(null)

// TODO: 待后端「意向/位次查询」接口（李帆）就绪后实现；当前仅完成页面结构。
// 修改信息（US-08）、撤销意向（US-09）依赖后端接口，且不在本轮 Sprint 清单，后期再加。
async function query() {
  if (!code.value.trim()) {
    ElMessage.warning('请输入口令码')
    return
  }
  loading.value = true
  try {
    // result.value = await 查询意向接口(code.value.trim())
    ElMessage.info('待后端「意向查询」接口对接')
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