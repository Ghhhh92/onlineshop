<template>
  <div class="home-container">
    <el-card v-if="product">
      <el-alert
        v-if="isFrozen"
        title="商品交易中，暂不可购买"
        type="warning"
        :closable="false"
        show-icon
        class="trade-tip"
      />
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
      <el-button type="primary" size="large" :disabled="isFrozen" @click="goBuy">
        {{ isFrozen ? '商品交易中' : '我要购买' }}
      </el-button>
    </el-card>
    <el-card v-else>
      <el-empty description="暂无在售商品" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOnSaleProduct } from '@/api/product'

const router = useRouter()
const product = ref(null)
const imageError = ref(false)
const isFrozen = computed(() => product.value?.status === 'FROZEN')

onMounted(async () => {
  try {
    product.value = await getOnSaleProduct()
  } catch {
    product.value = null
  }
})

function goBuy() {
  if (!isFrozen.value) {
    router.push('/buy')
  }
}
</script>

<style scoped>
.home-container {
  max-width: 800px;
  margin: 60px auto;
  padding: 0 20px;
}
.trade-tip {
  margin-bottom: 16px;
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