<template>
  <SellerLayout
    ><div class="page-heading">
      <div>
        <h1>发布商品</h1>
        <div class="muted">同一时间仅允许一件在售或冻结商品。</div>
      </div>
    </div>
    <el-alert
      v-if="existing"
      title="已有在售或冻结商品，请先处理当前商品后再发布。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="checkError"
      title="商品状态检查失败，请重试后发布。"
      type="error"
      :closable="false"
      ><el-button @click="check">重新检查</el-button></el-alert
    >
    <div class="publish-grid">
      <section class="panel" v-loading="checking">
        <el-form
          novalidate
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <el-form-item label="商品名称" prop="name"
            ><el-input
              v-model="form.name"
              maxlength="100"
              show-word-limit
              placeholder="例如：九成新台灯"
          /></el-form-item>
          <el-form-item label="商品描述（选填）" prop="description"
            ><el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="介绍成色、规格及交易方式"
          /></el-form-item>
          <el-form-item label="图片地址（选填）" prop="image"
            ><el-input
              v-model="form.image"
              maxlength="500"
              placeholder="https://…"
          /></el-form-item>
          <el-form-item label="售价（元）" prop="price"
            ><el-input-number
              v-model="form.price"
              :min="0.01"
              :max="99999999.99"
              :precision="2"
              :step="1"
              :controls="false"
          /></el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="checking || checkError || !!existing"
            >确认发布</el-button
          ><el-button @click="$router.push('/seller/admin')"
            >返回后台</el-button
          >
        </el-form>
      </section>
      <section class="panel">
        <h2>商品预览</h2>
        <el-image
          v-if="form.image && validImage(form.image)"
          class="product-image"
          :src="form.image"
          fit="cover"
          ><template #error>图片加载失败</template></el-image
        >
        <h3>{{ form.name.trim() || "商品名称" }}</h3>
        <p class="price">
          ￥{{ form.price ? Number(form.price).toFixed(2) : "0.00" }}
        </p>
        <p class="description">
          {{ form.description || "商品描述将显示在这里" }}
        </p>
        <p class="muted">
          发布后不支持修改，请核对名称、图片及售价。图片填写公开可访问的 HTTP /
          HTTPS 地址。
        </p>
        <p v-if="!complete" class="muted">冻结状态由发布接口最终校验。</p>
      </section>
    </div>
  </SellerLayout>
</template>
<script setup>
import { reactive, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import SellerLayout from "@/components/SellerLayout.vue";
import { publishProduct } from "@/api/product";
import { getCurrentProduct } from "@/api/sellerWorkspace";
const router = useRouter(),
  formRef = ref(),
  loading = ref(false),
  checking = ref(false),
  checkError = ref(false),
  existing = ref(null),
  complete = ref(true);
const form = reactive({
  name: "",
  description: "",
  image: "",
  price: undefined,
});
function validImage(value) {
  try {
    return ["http:", "https:"].includes(new URL(value.trim()).protocol);
  } catch {
    return false;
  }
}
const rules = {
  name: [
    {
      required: true,
      whitespace: true,
      message: "请输入商品名称",
      trigger: "blur",
    },
    { max: 100, message: "名称最多 100 个字符", trigger: "blur" },
  ],
  price: [
    {
      validator: (_, value, cb) =>
        cb(
          typeof value !== "number" ||
            !Number.isFinite(value) ||
            value < 0.01 ||
            value > 99999999.99
            ? new Error("请输入 0.01～99999999.99 元的价格")
            : undefined,
        ),
      trigger: "change",
    },
  ],
  image: [
    {
      validator: (_, value, cb) =>
        cb(
          value.trim() && !validImage(value)
            ? new Error("请填写 HTTP / HTTPS 图片地址")
            : undefined,
        ),
      trigger: "blur",
    },
  ],
};
async function check() {
  checking.value = true;
  checkError.value = false;
  try {
    const result = await getCurrentProduct();
    existing.value = result.product;
    complete.value = result.complete;
  } catch {
    checkError.value = true;
  } finally {
    checking.value = false;
  }
}
async function submit() {
  if (
    loading.value ||
    checking.value ||
    checkError.value ||
    existing.value ||
    !(await formRef.value.validate().catch(() => false))
  )
    return;
  loading.value = true;
  try {
    await publishProduct({
      ...form,
      name: form.name.trim(),
      image: form.image.trim(),
      description: form.description.trim(),
    });
    ElMessage.success("商品发布成功");
    await router.push("/seller/admin");
  } catch {
    await check();
  } finally {
    loading.value = false;
  }
}
onMounted(check);
</script>
