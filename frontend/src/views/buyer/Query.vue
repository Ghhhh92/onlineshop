<template>
  <BuyerLayout>
    <div class="query-container">
      <section class="query-heading">
        <h1>口令查询</h1>
        <p class="buyer-muted">输入提交意向时获得的口令码，即可查询排队位次与交易状态。</p>
      </section>

      <el-card>
        <div class="query-bar">
          <el-input
            v-model="code"
            placeholder="请输入购买意向的口令码"
            clearable
            size="large"
            class="code-input"
            @keyup.enter="query"
          />
          <el-button type="primary" size="large" :loading="loading" @click="query">
            查询
          </el-button>
        </div>

        <el-empty
          v-if="!result"
          description="输入口令码后查询排队位次与交易状态"
        />

        <el-descriptions v-else :column="1" border class="result-panel">
          <el-descriptions-item label="排队位次">
            <span class="position-num">{{ result.position }}</span> 位
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="result.status === 'TRADING' ? 'warning' : 'primary'" effect="light">
              {{ result.statusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ result.productName }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ result.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ result.phone }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ result.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
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

onMounted(() => {
  if (code.value) query()
})
</script>

<style scoped>
.query-container {
  max-width: 640px;
  margin: 0 auto;
}

.query-heading {
  text-align: center;
  padding: 10px 0 26px;
}

.query-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.query-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.code-input {
  flex: 1;
}

.result-panel {
  margin-top: 4px;
}

.position-num {
  color: #168675;
  font-size: 20px;
  font-weight: 800;
}

@media (max-width: 520px) {
  .query-bar {
    flex-direction: column;
  }
}
</style>