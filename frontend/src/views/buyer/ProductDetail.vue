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
            <el-carousel v-if="product.images && product.images.length" :autoplay="false" arrow="always" height="360px">
              <el-carousel-item v-for="(img, i) in product.images" :key="i">
                <img :src="img" :alt="product.name" class="gallery-img" />
              </el-carousel-item>
            </el-carousel>
            <div v-else class="gallery-empty">暂无图片</div>
          </div>

          <div class="info">
            <el-tag :type="available ? 'success' : 'info'" effect="light" round>
              {{ product.statusText || product.status }}
            </el-tag>
            <h2>{{ product.name }}</h2>
            <p class="price">￥{{ product.price }}</p>
            <p v-if="available" class="stock">库存 {{ product.stock }} 件</p>
            <p v-else class="stock">该商品当前不可购买</p>

            <div class="buyer-actions">
              <div class="quantity-row">
                <span class="quantity-label">数量</span>
                <el-input-number v-model="quantity" :min="1" :max="999" :disabled="!available" />
              </div>
              <el-button
                type="primary"
                size="large"
                class="action-btn"
                :disabled="!available"
                :loading="cartLoading"
                @click="handleAddCart"
              >
                加入购物车
              </el-button>
              <el-button
                size="large"
                class="action-btn"
                :disabled="!available"
                :loading="favoriteLoading"
                @click="handleFavorite"
              >
                收藏
              </el-button>
            </div>
          </div>
        </div>

        <div class="desc">
          <h3>商品描述</h3>
          <RichDescription v-if="product.description" :value="product.description" />
          <p v-else class="buyer-muted">暂无描述</p>
        </div>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import RichDescription from '@/components/RichDescription.vue'

import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getProductDetail } from '@/api/product'
import { addCart, addFavorite } from '@/api/customer'
import { useCustomerStore } from '@/stores/customer'
import { ensureCustomerLogin } from '@/utils/ensureCustomer'

const route = useRoute()
const router = useRouter()
const customer = useCustomerStore()
const product = ref(null)
const loading = ref(false)
const quantity = ref(1)
const cartLoading = ref(false)
const favoriteLoading = ref(false)

const available = computed(() => product.value?.status === 'ON_SALE')

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

async function handleAddCart() {
  if (!available.value) return
  if (!(await ensureCustomerLogin(customer, router))) return
  cartLoading.value = true
  try {
    await addCart(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    cartLoading.value = false
  }
}

async function handleFavorite() {
  if (!available.value) return
  if (!(await ensureCustomerLogin(customer, router))) return
  favoriteLoading.value = true
  try {
    await addFavorite(product.value.id)
    ElMessage.success('已收藏')
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    favoriteLoading.value = false
  }
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

.gallery > .el-carousel {
  width: 100%;
  min-width: 0;
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

.buyer-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quantity-label {
  color: #61736e;
  font-weight: 600;
}

.action-btn {
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