<template>
  <div class="admin-container">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h2 style="margin: 0">卖家后台</h2>
          <el-button @click="goPublish">发布商品</el-button>
        </div>
      </template>
      <el-empty description="暂无在售商品" v-if="!product" />
      <div v-else>
        <h3>{{ product.name }}</h3>
        <p>价格：￥{{ product.price }}</p>
        <p>状态：{{ statusText }}</p>
        <p v-if="product.description">描述：{{ product.description }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOnSaleProduct } from '@/api/product'

const router = useRouter()
const product = ref(null)

const statusText = computed(() => {
  const map = { ON_SALE: '在售', FROZEN: '交易中', DELISTED: '已下架' }
  return product.value ? map[product.value.status] || product.value.status : ''
})

onMounted(async () => {
  product.value = await getOnSaleProduct()
})

function goPublish() {
  router.push('/seller/publish')
}
</script>

<style scoped>
.admin-container {
  padding: 40px;
}
</style>
