<template>
  <BuyerLayout>
    <div class="done-container">
      <el-card>
        <div class="done-inner">
          <div class="result-icon">✓</div>
          <h2>提交成功</h2>
          <p class="result-sub">请妥善保管口令码，用于查询排队状态。</p>

          <div class="code-box">
            <span class="code-label">您的口令码</span>
            <span class="code">{{ code }}</span>
            <el-button text type="primary" @click="copyCode">复制</el-button>
          </div>

          <p v-if="position" class="position">当前排第 {{ position }} 位</p>

          <div class="actions">
            <el-button type="primary" size="large" @click="copyCode">复制口令码</el-button>
            <el-button size="large" @click="goQuery">查询排队状态</el-button>
            <el-button size="large" text @click="goHome">返回首页</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'

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

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.done-container {
  max-width: 560px;
  margin: 20px auto;
}

.done-inner {
  text-align: center;
  padding: 14px 8px;
}

.result-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #23a892, #0f6b5e);
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 18px;
}

.done-inner h2 {
  margin: 0 0 8px;
  font-size: 26px;
}

.result-sub {
  margin: 0 auto 24px;
  color: #71817c;
}

.code-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  max-width: 460px;
  margin: 0 auto;
  padding: 16px 20px;
  background: #f0f7f5;
  border: 1px dashed #9fd3c9;
  border-radius: 12px;
}

.code-label {
  color: #61736e;
  font-size: 13px;
}

.code {
  font-size: 26px;
  font-weight: 800;
  color: #168675;
  letter-spacing: 2px;
  word-break: break-all;
}

.position {
  margin: 22px 0 0;
  color: #168675;
  font-size: 16px;
  font-weight: 700;
}

.actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>