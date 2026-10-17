<template>
  <div class="home-container">
    <el-card v-if="product">
      <div class="product-image">
        <img
          v-if="product.image && !imageError"
          :src="product.image"
          alt="商品图片"
          @error="imageError = true"
        />
        <div v-else class="image-placeholder">暂无图片</div>
      </div>
      <h2>{{ product.name }}</h2>
      <p class="price">价格：￥{{ product.price }}</p>
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
const imageError = ref(false)

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
.product-image {
  margin-bottom: 16px;
}
.product-image img {
  max-width: 100%;
  max-height: 360px;
  border-radius: 6px;
}
.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  background: #f5f7fa;
  color: #909399;
  border-radius: 6px;
}
.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 600;
}
</style>