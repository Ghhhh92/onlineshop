<template>
  <BuyerLayout>
    <div class="auth-container">
      <el-card class="auth-card">
        <h1>客户登录</h1>
        <p class="buyer-muted">登录后可在「我的订单」查看历史下单记录。</p>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="请输入密码"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>
        <p class="switch-line">
          还没有账号？
          <router-link to="/customer/register">去注册</router-link>
        </p>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { login } from '@/api/customer'
import { useCustomerStore } from '@/stores/customer'

const router = useRouter()
const store = useCustomerStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const customer = await login({ username: form.username.trim(), password: form.password })
    store.setLogin(customer)
    ElMessage.success('登录成功')
    router.push('/customer/orders')
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 10px 18px 18px;
}

.auth-card h1 {
  margin: 8px 0 6px;
  font-size: 26px;
}

.submit-btn {
  width: 100%;
}

.switch-line {
  margin: 18px 0 4px;
  text-align: center;
  color: #71817c;
  font-size: 14px;
}

.switch-line a {
  color: #168675;
  text-decoration: none;
  font-weight: 600;
}
</style>