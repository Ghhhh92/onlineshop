<template>
  <BuyerLayout>
    <div class="buy-container">
      <section class="buy-heading">
        <h1>提交购买意向</h1>
        <p class="buyer-muted">买家免注册，填写姓名与联系电话即可进入排队，系统会返回唯一口令码。</p>
      </section>

      <el-card>
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
              maxlength="30"
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { submitIntent } from '@/api/buyer'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
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
    const data = await submitIntent({ name: form.name.trim(), phone: form.phone.trim() })
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

.buy-form {
  padding: 8px 6px;
}

.submit-btn {
  width: 100%;
}
</style>