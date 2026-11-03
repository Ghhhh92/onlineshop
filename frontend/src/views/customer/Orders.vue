<template>
  <BuyerLayout>
    <div class="orders-container">
      <section class="orders-heading">
        <h1>我的订单</h1>
        <p class="buyer-muted">你登录后提交的购买意向，交易成功与失败的记录都会保留。</p>
      </section>

      <div v-if="loading" class="orders-state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty v-else-if="!orders.length" description="还没有下单记录" />

      <div v-else class="order-list">
        <el-card v-for="item in orders" :key="item.intentId" class="order-item">
          <div class="order-row">
            <div class="order-cover">
              <img v-if="item.image" :src="item.image" :alt="item.productName" />
              <div v-else class="cover-placeholder">暂无图片</div>
            </div>
            <div class="order-meta">
              <h3>{{ item.productName || '商品已下架' }}</h3>
              <p class="order-price">￥{{ item.price }}</p>
              <p class="order-time">下单时间：{{ item.createTime }}</p>
            </div>
            <div class="order-status">
              <el-tag :type="statusType(item.status)" effect="light">{{ item.statusText }}</el-tag>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="changePage"
        />
      </div>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getOrders } from '@/api/customer'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

function statusType(status) {
  return {
    SUCCESS: 'success',
    QUEUING: 'primary',
    TRADING: 'warning',
    FAILED: 'danger',
    CANCELED: 'info',
  }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await getOrders({ page: page.value, size })
    orders.value = data.records || []
    total.value = data.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

onMounted(load)
</script>

<style scoped>
.orders-container {
  max-width: 760px;
  margin: 0 auto;
}

.orders-heading {
  text-align: center;
  padding: 10px 0 24px;
}

.orders-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.orders-state {
  min-height: 300px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-item :deep(.el-card__body) {
  padding: 16px 18px;
}

.order-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-cover {
  width: 84px;
  height: 84px;
  border-radius: 10px;
  overflow: hidden;
  background: #f0f5f3;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: #82928c;
  font-size: 12px;
}

.order-meta {
  flex: 1;
  min-width: 0;
}

.order-meta h3 {
  margin: 0 0 6px;
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-price {
  margin: 0 0 6px;
  color: #168675;
  font-weight: 700;
}

.order-time {
  margin: 0;
  color: #8a9894;
  font-size: 13px;
}

.order-status {
  flex-shrink: 0;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>