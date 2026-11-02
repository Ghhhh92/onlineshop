<template>
  <BuyerLayout>
    <div class="home-container">
      <section class="hero">
        <p class="hero-kicker">ONLINE SHOP</p>
        <h1>发现值得入手的好物</h1>
        <p class="hero-sub">浏览在售商品，提交购买意向后凭口令码实时查询排队进度。</p>
      </section>

      <el-card v-if="product" class="product-card">
        <el-alert
          v-if="isFrozen"
          title="商品交易中，暂不可购买"
          type="warning"
          :closable="false"
          show-icon
          class="trade-tip"
        />
        <div class="product-body">
          <div class="product-image">
            <img
              v-if="product.image && !imageError"
              :src="product.image"
              alt="商品图片"
              @error="imageError = true"
            />
            <div v-else class="image-placeholder">暂无图片</div>
          </div>
          <div class="product-info">
            <el-tag :type="isFrozen ? 'warning' : 'success'" effect="light" round>
              {{ product.statusText || '在售' }}
            </el-tag>
            <h2>{{ product.name }}</h2>
            <p class="price">￥{{ product.price }}</p>
            <p v-if="product.description" class="description">
              {{ product.description }}
            </p>
            <el-button
              type="primary"
              size="large"
              :disabled="isFrozen"
              @click="goBuy"
            >
              {{ isFrozen ? '商品交易中' : '我要购买' }}
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card v-else class="empty-card">
        <el-empty description="暂无在售商品">
          <el-button @click="load">刷新看看</el-button>
        </el-empty>
      </el-card>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getOnSaleProduct } from '@/api/product'

const router = useRouter()
const product = ref(null)
const imageError = ref(false)
const isFrozen = computed(() => product.value?.status === 'FROZEN')

async function load() {
  imageError.value = false
  try {
    product.value = await getOnSaleProduct()
  } catch {
    product.value = null
  }
}

onMounted(load)

function goBuy() {
  if (!isFrozen.value) {
    router.push('/buy')
  }
}
</script>

<style scoped>
.home-container {
  max-width: 860px;
  margin: 0 auto;
}

.hero {
  text-align: center;
  padding: 22px 0 34px;
}

.hero-kicker {
  margin: 0 0 10px;
  letter-spacing: 0.28em;
  font-size: 12px;
  font-weight: 700;
  color: #168675;
}

.hero h1 {
  margin: 0 0 12px;
  font-size: clamp(26px, 5vw, 38px);
  letter-spacing: 1px;
}

.hero-sub {
  margin: 0 auto;
  max-width: 560px;
  color: #71817c;
  line-height: 1.7;
}

.product-card {
  overflow: hidden;
}

.trade-tip {
  margin: 18px 22px 0;
}

.product-body {
  display: grid;
  grid-template-columns: minmax(0, 300px) 1fr;
  gap: 28px;
  padding: 24px;
  align-items: center;
}

.product-image {
  aspect-ratio: 1;
  border-radius: 14px;
  overflow: hidden;
  background: #f0f5f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  color: #82928c;
}

.product-info h2 {
  margin: 14px 0 10px;
  font-size: 26px;
}

.price {
  margin: 0 0 12px;
  color: #168675;
  font-size: 30px;
  font-weight: 800;
}

.description {
  margin: 0 0 20px;
  color: #71817c;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.empty-card {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 700px) {
  .product-body {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .product-image {
    max-width: 320px;
    width: 100%;
    margin: 0 auto;
  }
}
</style>