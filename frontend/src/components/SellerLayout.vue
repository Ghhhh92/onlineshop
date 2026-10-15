<template>
  <div class="seller-shell">
    <header class="seller-header">
      <router-link class="brand" to="/seller/admin"
        >小店 · 卖家工作台</router-link
      >
      <div class="account">
        <span>{{ store.username }}</span
        ><el-button text @click="passwordVisible = true">修改密码</el-button
        ><el-button text :loading="leaving" @click="signOut"
          >退出登录</el-button
        >
      </div>
    </header>
    <nav class="seller-nav" aria-label="卖家导航">
      <router-link to="/seller/admin">后台首页</router-link
      ><router-link to="/seller/publish">发布商品</router-link
      ><router-link to="/seller/history">历史商品</router-link
      ><router-link to="/">买家首页 ↗</router-link>
    </nav>
    <main class="seller-main"><slot /></main>
    <el-dialog
      v-model="passwordVisible"
      title="修改登录密码"
      width="min(440px, 92vw)"
      :close-on-click-modal="false"
    >
      <el-form
        novalidate
        ref="passwordRef"
        :model="password"
        :rules="rules"
        label-position="top"
        @submit.prevent="savePassword"
      >
        <el-form-item label="原密码" prop="oldPassword"
          ><el-input
            v-model="password.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
        /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"
          ><el-input
            v-model="password.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            maxlength="64"
        /></el-form-item>
        <el-form-item label="确认新密码" prop="confirm"
          ><el-input
            v-model="password.confirm"
            type="password"
            show-password
            autocomplete="new-password"
        /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving"
          >保存并重新登录</el-button
        >
      </el-form>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { logout, changePassword } from "@/api/seller";
import { useSellerStore } from "@/stores/seller";
const store = useSellerStore(),
  router = useRouter();
const leaving = ref(false),
  saving = ref(false),
  passwordVisible = ref(false),
  passwordRef = ref();
const password = reactive({ oldPassword: "", newPassword: "", confirm: "" });
const rules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    {
      required: true,
      min: 8,
      max: 64,
      message: "新密码需为 8～64 个字符",
      trigger: "blur",
    },
    {
      validator: (_, value, cb) =>
        cb(
          value === password.oldPassword
            ? new Error("新旧密码不能相同")
            : undefined,
        ),
      trigger: "blur",
    },
  ],
  confirm: [
    {
      validator: (_, value, cb) =>
        cb(
          value !== password.newPassword || !value
            ? new Error("两次新密码不一致")
            : undefined,
        ),
      trigger: "blur",
    },
  ],
};
watch(passwordVisible, () => {
  Object.assign(password, { oldPassword: "", newPassword: "", confirm: "" });
  passwordRef.value?.clearValidate();
});
async function signOut() {
  if (leaving.value) return;
  leaving.value = true;
  try {
    await logout();
    store.logout();
    await router.replace("/seller/login");
  } catch {
  } finally {
    leaving.value = false;
  }
}
async function savePassword() {
  if (saving.value || !(await passwordRef.value.validate().catch(() => false)))
    return;
  saving.value = true;
  try {
    await changePassword({
      oldPassword: password.oldPassword,
      newPassword: password.newPassword,
    });
    store.logout();
    ElMessage.success("密码已修改，请重新登录");
    await router.replace("/seller/login");
  } catch {
  } finally {
    saving.value = false;
  }
}
</script>
