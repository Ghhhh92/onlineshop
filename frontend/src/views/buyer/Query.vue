<template>
  <BuyerLayout>
    <div class="query-container">
      <section class="query-heading">
        <h1>口令查询</h1>
        <p class="buyer-muted">输入提交意向时获得的口令码，即可查询排队位次，排队中还可以撤销意向。</p>
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

        <template v-else>
          <el-descriptions :column="1" border class="result-panel">
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

          <div v-if="result.status === 'QUEUING'" class="cancel-bar">
            <el-button type="danger" plain :loading="canceling" @click="cancel">撤销意向</el-button>
          </div>
        </template>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getIntentByCode, cancelIntent } from '@/api/buyer'

const route = useRoute()
const code = ref(route.query.code || '')
const loading = ref(false)
const canceling = ref(false)
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

async function cancel() {
  if (!result.value) return
  try {
    await ElMessageBox.confirm('撤销后将退出排队，口令码失效，确定要撤销吗？', '撤销意向', {
      type: 'warning',
      confirmButtonText: '确定撤销',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }
  canceling.value = true
  try {
    await cancelIntent(result.value.code)
    ElMessage.success('已撤销意向')
    await query()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    canceling.value = false
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

.cancel-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 520px) {
  .query-bar {
    flex-direction: column;
  }
}
</style>