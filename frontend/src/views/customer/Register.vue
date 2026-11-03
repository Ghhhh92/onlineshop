<template>
  <BuyerLayout>
    <div class="auth-container">
      <el-card class="auth-card">
        <h1>客户注册</h1>
        <p class="buyer-muted">注册后可用账号认领购买意向，并查看历史下单记录。</p>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="2～20 位中文、字母、数字或下划线" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" size="large" show-password placeholder="8～64 个字符" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirm">
            <el-input v-model="form.confirm" type="password" size="large" show-password placeholder="再次输入密码" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" size="large" placeholder="11 位手机号" clearable maxlength="11" />
          </el-form-item>
          <el-form-item label="默认交易地点" prop="defaultLocation">
            <el-input v-model="form.defaultLocation" size="large" placeholder="例如：图书馆门口" clearable maxlength="100" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            注册
          </el-button>
        </el-form>
        <p class="switch-line">
          已有账号？
          <router-link to="/customer/login">去登录</router-link>
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
import { register } from '@/api/customer'
import { useCustomerStore } from '@/stores/customer'

const router = useRouter()
const store = useCustomerStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', confirm: '', phone: '', defaultLocation: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\u4e00-\u9fa5]{2,20}$/, message: '2～20 位中文、字母、数字或下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8～64 个字符', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, cb) => cb(value !== form.password ? new Error('两次输入的密码不一致') : undefined),
      trigger: 'blur',
    },
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  defaultLocation: [{ required: true, message: '请输入默认交易地点', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const customer = await register({
      username: form.username.trim(),
      password: form.password,
      phone: form.phone.trim(),
      defaultLocation: form.defaultLocation.trim(),
    })
    store.setLogin(customer)
    ElMessage.success('注册成功，已自动登录')
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
  max-width: 460px;
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