<template>
  <BuyerLayout>
    <div class="buy-container">
      <section class="buy-heading">
        <h1>提交购买意向</h1>
        <p class="buyer-muted">填写姓名与联系电话即可进入排队；已登录客户的意向会自动记入账号。</p>
      </section>

      <el-card>
        <el-alert
          v-if="productId"
          :title="`正在购买：${productName || '商品 #' + productId}`"
          type="info"
          :closable="false"
          show-icon
          class="buy-alert"
        />
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="buy-form"
          @submit.prevent
        >
          <el-form-item label="姓名" prop="name">
            <el-input
              v-model="form.name"
              size="large"
              placeholder="请填写真实姓名"
              clearable
              maxlength="50"
            />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input
              v-model="form.phone"
              size="large"
              placeholder="请填写 11 位手机号"
              clearable
              maxlength="11"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
              提交购买意向
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { submitIntent } from '@/api/buyer'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const productId = computed(() => route.query.productId || null)
const productName = computed(() => route.query.productName || '')

const form = reactive({ name: '', phone: '' })
const rules = {
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload = { name: form.name.trim(), phone: form.phone.trim() }
    if (productId.value) payload.productId = productId.value
    const data = await submitIntent(payload)
    router.push({ path: `/done/${data.code}`, query: { position: data.position, createTime: data.createTime } })
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.buy-container {
  max-width: 560px;
  margin: 0 auto;
}

.buy-heading {
  text-align: center;
  padding: 10px 0 26px;
}

.buy-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.buy-alert {
  margin-bottom: 18px;
}

.buy-form {
  padding: 8px 6px;
}

.submit-btn {
  width: 100%;
}
</style>