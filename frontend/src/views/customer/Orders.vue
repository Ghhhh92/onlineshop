<template>
  <BuyerLayout>
    <div class="orders-container">
      <section class="orders-heading">
        <h1>我的订单</h1>
        <p class="buyer-muted">登录后查看订单状态；在卖家开始发货前，你可以取消订单。</p>
      </section>

      <div class="orders-filter">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 180px" @change="search">
          <el-option v-for="(info, key) in ORDER_STATUS" :key="key" :label="info.text" :value="key" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>

      <div v-if="loading" class="orders-state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty v-else-if="!orders.length" description="还没有订单" />

      <div v-else class="order-list">
        <el-card v-for="order in orders" :key="order.id" class="order-item">
          <div class="order-head">
            <div>
              <span class="order-no">订单号 {{ order.orderNo }}</span>
              <span class="order-time">{{ order.createTime }}</span>
            </div>
            <el-tag :type="orderStatusType(order.status)" effect="light">
              {{ orderStatusText(order.status) }}
            </el-tag>
          </div>

          <div class="order-body">
            <div class="order-items">
              <div v-for="item in order.items" :key="item.productId" class="order-line">
                <span class="line-name">{{ item.productName }}</span>
                <span class="line-qty">× {{ item.quantity }}</span>
              </div>
            </div>
            <div class="order-total">合计 ￥{{ Number(order.totalAmount || 0).toFixed(2) }}</div>
          </div>

          <div class="order-actions">
            <el-button size="small" @click="showDetail(order)">详情</el-button>
            <el-button
              v-if="customerCanCancel(order.status)"
              size="small"
              type="danger"
              plain
              @click="cancel(order)"
            >
              取消订单
            </el-button>
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

      <el-dialog v-model="detailVisible" title="订单详情" width="min(720px, 94vw)">
        <div v-loading="detailLoading">
          <p v-if="detail" class="detail-order-no">
            订单号：{{ detail.orderNo }} ｜ 状态：{{ orderStatusText(detail.status) }}
          </p>
          <el-table :data="detail?.items || []" empty-text="暂无明细">
            <el-table-column prop="productName" label="商品" min-width="180" />
            <el-table-column prop="price" label="单价（元）" width="120" />
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column label="小计（元）" width="130">
              <template #default="{ row }">{{ (Number(row.price || 0) * row.quantity).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
          <p v-if="detail" class="detail-total">订单金额：￥{{ Number(detail.totalAmount || 0).toFixed(2) }}</p>
        </div>
      </el-dialog>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getOrderList, getOrderDetail, cancelCustomerOrder } from '@/api/customer'
import {
  ORDER_STATUS,
  orderStatusText,
  orderStatusType,
  customerCanCancel,
} from '@/utils/orderStatus'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const status = ref('')
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await getOrderList({
      page: page.value,
      size,
      status: status.value || undefined,
    })
    orders.value = data.records || []
    total.value = data.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(p) {
  page.value = p
  load()
}

async function showDetail(order) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getOrderDetail(order.id)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function cancel(order) {
  try {
    await cancelCustomerOrder(order.id)
    ElMessage.success('订单已取消')
    await load()
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.orders-container {
  max-width: 820px;
  margin: 0 auto;
}

.orders-heading {
  text-align: center;
  padding: 10px 0 18px;
}

.orders-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.orders-filter {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
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

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef3f1;
}

.order-no {
  font-weight: 700;
}

.order-time {
  margin-left: 14px;
  color: #8a9894;
  font-size: 13px;
}

.order-body {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
}

.order-items {
  flex: 1;
  min-width: 0;
}

.order-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 3px 0;
}

.line-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.line-qty {
  color: #61736e;
  flex-shrink: 0;
}

.order-total {
  flex-shrink: 0;
  color: #168675;
  font-weight: 800;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.detail-order-no {
  margin: 0 0 14px;
  color: #61736e;
}

.detail-total {
  margin: 16px 0 0;
  text-align: right;
  color: #168675;
  font-weight: 800;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>