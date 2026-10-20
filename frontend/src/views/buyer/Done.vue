<template>
  <div class="done-container">
    <el-card>
      <h2>提交成功</h2>
      <p>您的口令码：</p>
      <p class="code">{{ code }}</p>
      <p v-if="position" class="position">当前排第 {{ position }} 位</p>
      <p class="tip">请妥善保管口令码，用于查询排队状态。</p>
      <div class="actions">
        <el-button type="primary" @click="copyCode">复制口令码</el-button>
        <el-button @click="goQuery">去查询排队状态</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const code = computed(() => route.params.code)
const position = computed(() => route.query.position || '')

async function copyCode() {
  try {
    await navigator.clipboard.writeText(String(code.value))
    ElMessage.success('口令码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动记录')
  }
}

function goQuery() {
  router.push({ path: '/query', query: { code: code.value } })
}
</script>

<style scoped>
.done-container {
  max-width: 600px;
  margin: 100px auto;
  text-align: center;
}
.code {
  font-size: 28px;
  font-weight: 700;
  color: #12b5a5;
  letter-spacing: 2px;
}
.position {
  color: #f56c6c;
  font-size: 16px;
}
.tip {
  color: #909399;
}
.actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>