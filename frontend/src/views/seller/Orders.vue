<template>
  <SellerLayout>
    <div class="page-heading">
      <h1>订单管理</h1>
      <el-button @click="load">刷新</el-button>
    </div>

    <section class="panel">
      <el-form inline @submit.prevent="search">
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部状态" style="width: 160px">
            <el-option v-for="(info, key) in ORDER_STATUS" :key="key" :label="info.text" :value="key" />
          </el-select>
        </el-form-item>
        <el-button native-type="submit" type="primary">查询</el-button>
      </el-form>

      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table v-else :data="rows" v-loading="loading" empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" effect="light">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额（元）" width="130">
          <template #default="{ row }">{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="180" />
        <el-table-column label="操作" min-width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="act(row, confirmOrder)">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="primary" @click="act(row, prepareOrder)">备货完成</el-button>
            <el-button v-if="row.status === 'PREPARED'" link type="primary" @click="act(row, shipOrder)">开始发货</el-button>
            <el-button v-if="row.status === 'SHIPPED'" link type="primary" @click="act(row, completeOrder)">交易完成</el-button>
            <el-button v-if="sellerCanCancel(row.status)" link type="danger" @click="act(row, cancelSellerOrder, '确认取消该订单？')">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </section>

    <el-dialog v-model="detailVisible" title="订单详情" width="min(760px, 94vw)">
      <div v-loading="detailLoading">
        <p v-if="detail" class="detail-no">
          订单号：{{ detail.orderNo }} ｜ 状态：{{ orderStatusText(detail.status) }}
        </p>
        <el-table :data="detail?.items || []" empty-text="暂无明细">
          <el-table-column prop="productName" label="商品" min-width="200" />
          <el-table-column prop="price" label="单价（元）" width="120" />
          <el-table-column prop="quantity" label="数量" width="90" />
          <el-table-column label="小计（元）" width="130">
            <template #default="{ row }">{{ (Number(row.price || 0) * row.quantity).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
        <p v-if="detail" class="detail-total">订单金额：￥{{ Number(detail.totalAmount || 0).toFixed(2) }}</p>
      </div>
    </el-dialog>
  </SellerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SellerLayout from '@/components/SellerLayout.vue'
import {
  getSellerOrders,
  getSellerOrder,
  confirmOrder,
  prepareOrder,
  shipOrder,
  completeOrder,
  cancelSellerOrder,
} from '@/api/sellerWorkspace'
import {
  ORDER_STATUS,
  orderStatusText,
  orderStatusType,
  sellerCanCancel,
} from '@/utils/orderStatus'

const rows = ref([])
const page = ref(1)
const total = ref(0)
const status = ref('')
const loading = ref(false)
const error = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

let request = 0

async function load() {
  const token = ++request
  loading.value = true
  error.value = ''
  try {
    const data = await getSellerOrders({
      page: page.value,
      size: 10,
      status: status.value || undefined,
    })
    if (token === request) {
      rows.value = data.records || []
      total.value = data.total || 0
    }
  } catch {
    if (token === request) error.value = '订单加载失败，请刷新重试'
  } finally {
    if (token === request) loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

async function showDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getSellerOrder(row.id)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function act(row, api, confirmText) {
  if (confirmText) {
    try {
      await ElMessageBox.confirm(confirmText, '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  try {
    await api(row.id)
    ElMessage.success('操作成功')
    await load()
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.detail-no {
  margin: 0 0 14px;
  color: #61736e;
}

.detail-total {
  margin: 16px 0 0;
  text-align: right;
  color: #168675;
  font-weight: 800;
}
</style>