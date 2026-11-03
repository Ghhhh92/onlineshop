<template>
  <BuyerLayout>
    <div class="detail-container">
      <div v-if="loading" class="detail-state">
        <el-skeleton :rows="6" animated />
      </div>

      <el-empty v-else-if="!product" description="商品不存在或已下架" />

      <el-card v-else class="detail-card">
        <div class="detail-grid">
          <div class="gallery">
            <el-carousel v-if="product.images && product.images.length" :interval="5000" height="360px">
              <el-carousel-item v-for="(img, i) in product.images" :key="i">
                <img :src="img" :alt="product.name" class="gallery-img" />
              </el-carousel-item>
            </el-carousel>
            <div v-else class="gallery-empty">暂无图片</div>
          </div>

          <div class="info">
            <el-tag :type="isFrozen ? 'warning' : 'success'" effect="light" round>
              {{ product.statusText }}
            </el-tag>
            <h2>{{ product.name }}</h2>
            <p class="price">￥{{ product.price }}</p>
            <p v-if="product.status === 'ON_SALE'" class="stock">库存 {{ product.stock }} 件</p>
            <el-button
              type="primary"
              size="large"
              class="buy-btn"
              :disabled="isFrozen"
              @click="goBuy"
            >
              {{ isFrozen ? '商品交易中' : '我要购买' }}
            </el-button>
          </div>
        </div>

        <div class="desc">
          <h3>商品描述</h3>
          <div v-if="product.description" class="rich-text" v-html="product.description" />
          <p v-else class="buyer-muted">暂无描述</p>
        </div>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getProductDetail } from '@/api/product'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const loading = ref(false)
const isFrozen = computed(() => product.value?.status === 'FROZEN')

onMounted(async () => {
  loading.value = true
  try {
    product.value = await getProductDetail(route.params.id)
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
})

function goBuy() {
  if (isFrozen.value) return
  router.push({ path: '/buy', query: { productId: product.value.id } })
}
</script>

<style scoped>
.detail-container {
  max-width: 960px;
  margin: 0 auto;
}

.detail-state {
  min-height: 320px;
}

.detail-card :deep(.el-card__body) {
  padding: 26px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 340px);
  gap: 30px;
}

.gallery {
  border-radius: 14px;
  overflow: hidden;
  background: #f0f5f3;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gallery-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-empty {
  color: #82928c;
}

.info h2 {
  margin: 16px 0 12px;
  font-size: 28px;
}

.price {
  margin: 0 0 14px;
  color: #168675;
  font-size: 32px;
  font-weight: 800;
}

.stock {
  margin: 0 0 18px;
  color: #71817c;
}

.buy-btn {
  width: 100%;
}

.desc {
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid #eef3f1;
}

.desc h3 {
  margin: 0 0 14px;
  font-size: 18px;
}

@media (max-width: 720px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>