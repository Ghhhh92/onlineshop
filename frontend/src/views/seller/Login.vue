<template>
  <main class="seller-login">
    <section class="login-card">
      <span class="muted">ONLINE SHOP / 卖家中心</span>
      <h1>欢迎回来</h1>
      <p class="muted">登录工作台，管理商品与购买意向。</p>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
      />
      <el-form
        novalidate
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
      >
        <el-form-item label="用户名" prop="username"
          ><el-input
            v-model="form.username"
            autocomplete="username"
            maxlength="50"
            placeholder="请输入用户名"
        /></el-form-item>
        <el-form-item label="密码" prop="password"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
        /></el-form-item>
        <el-button
          type="primary"
          size="large"
          native-type="submit"
          :loading="loading"
          >登录工作台</el-button
        >
      </el-form>
      <p class="muted"><router-link to="/">返回买家首页</router-link></p>
    </section>
  </main>
</template>
<script setup>
import { ref, reactive } from "vue";
import { useRouter, useRoute } from "vue-router";
import { login } from "@/api/seller";
import { useSellerStore } from "@/stores/seller";
const router = useRouter(),
  store = useSellerStore(),
  formRef = ref(),
  loading = ref(false),
  error = ref("");
if (useRoute().query.reason === "connection")
  error.value = "无法确认登录状态，请检查后端连接后重试。";
const form = reactive({ username: "", password: "" });
const rules = {
  username: [
    {
      required: true,
      whitespace: true,
      message: "请输入用户名",
      trigger: "blur",
    },
  ],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};
async function submit() {
  if (loading.value || !(await formRef.value.validate().catch(() => false)))
    return;
  loading.value = true;
  error.value = "";
  try {
    const data = await login({
      username: form.username.trim(),
      password: form.password,
    });
    store.setLogin(data.username);
    await router.replace("/seller/admin");
  } catch (err) {
    error.value =
      err.response?.data?.message || err.message || "登录失败，请重试";
  } finally {
    loading.value = false;
  }
}
</script>
