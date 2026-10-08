<template>
  <div class="home-container">
    <el-card v-if="product">
      <h2>{{ product.name }}</h2>
      <p>价格：￥{{ product.price }}</p>
      <p v-if="product.description">描述：{{ product.description }}</p>
      <el-button type="primary" size="large" @click="goBuy">我要购买</el-button>
    </el-card>
    <el-card v-else>
      <el-empty description="暂无在售商品" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOnSaleProduct } from '@/api/product'

const router = useRouter()
const product = ref(null)

onMounted(async () => {
  product.value = await getOnSaleProduct()
})

function goBuy() {
  router.push('/buy')
}
</script>

<style scoped>
.home-container {
  max-width: 800px;
  margin: 60px auto;
  padding: 0 20px;
}
</style>
