<template>
  <SellerLayout
    ><div class="page-heading">
      <div>
        <h1>后台首页</h1>
        <div class="muted">查看当前商品，联系意向买家。</div>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert
      v-if="!complete && !loading && !error"
      title="当前仅能查询在售商品，冻结商品状态暂不可用。"
      type="warning"
      :closable="false"
      show-icon
    />
    <section v-loading="loading" class="panel">
      <h2>当前商品</h2>
      <div v-if="product" class="product-card">
        <el-image
          v-if="product.image"
          class="product-image"
          :src="product.image"
          fit="cover"
          ><template #error>图片加载失败</template></el-image
        >
        <div v-else class="product-image">暂无图片</div>
        <div>
          <el-tag :type="product.status === 'FROZEN' ? 'warning' : 'success'">{{
            product.status === "FROZEN" ? "已冻结 · 交易中" : "在售"
          }}</el-tag>
          <h2 style="margin-top: 16px">{{ product.name }}</h2>
          <p class="price">￥{{ Number(product.price).toFixed(2) }}</p>
          <p class="description">{{ product.description || "暂无描述" }}</p>
          <p class="muted">发布时间：{{ product.createTime || "—" }}</p>
        </div>
      </div>
      <el-empty
        v-else-if="!loading && !error"
        :description="
          complete ? '暂无在售或冻结商品' : '暂无在售商品，冻结状态暂无法确认'
        "
        ><el-button type="primary" @click="$router.push('/seller/publish')"
          >去发布商品</el-button
        ></el-empty
      >
    </section>
    <section class="panel">
      <h2>意向购买人</h2>
      <el-alert
        v-if="intentError"
        :title="intentError"
        type="info"
        :closable="false"
      /><IntentTable v-else :items="intents" />
    </section>
  </SellerLayout>
</template>
<script setup>
import { onMounted, ref } from "vue";
import SellerLayout from "@/components/SellerLayout.vue";
import IntentTable from "@/components/IntentTable.vue";
import { getCurrentProduct, getProductIntents } from "@/api/sellerWorkspace";
const product = ref(null),
  complete = ref(true),
  loading = ref(false),
  error = ref(""),
  intentError = ref(""),
  intents = ref([]);
async function load() {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  intentError.value = "";
  intents.value = [];
  product.value = null;
  try {
    const result = await getCurrentProduct();
    product.value = result.product;
    complete.value = result.complete;
    if (product.value) {
      try {
        intents.value = await getProductIntents(product.value.id);
      } catch (err) {
        intentError.value = [404, 405].includes(err.response?.status)
          ? "意向人查询暂不可用，等待服务接通。"
          : "意向人加载失败，请点击刷新重试。";
      }
    } else if (!complete.value)
      intentError.value = "尚无法确认冻结商品及其意向人。";
  } catch {
    error.value = "商品加载失败，请检查网络后刷新重试。";
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
