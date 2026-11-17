<template>
  <BuyerLayout>
    <div class="cart-container">
      <section class="cart-heading">
        <h1>购物车</h1>
        <p class="buyer-muted">勾选商品后可多选下单或转为收藏；库存不足会在下单时整单拦截并提示。</p>
      </section>

      <div v-if="loading" class="state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty v-else-if="!items.length" description="购物车还是空的" />

      <template v-else>
        <el-table :data="items" row-key="id" v-loading="loading" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="46" :selectable="(row) => row.available" />
          <el-table-column label="商品" min-width="240">
            <template #default="{ row }">
              <div class="cart-product">
                <div class="cart-cover">
                  <img v-if="row.image" :src="row.image" :alt="row.productName" />
                  <div v-else class="cover-placeholder">暂无图片</div>
                </div>
                <div class="cart-meta">
                  <h3>{{ row.productName || '商品已不存在' }}</h3>
                  <el-tag v-if="row.delisted" type="info" size="small" effect="light">已下架/无货</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="110">
            <template #default="{ row }">￥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="999"
                :disabled="!row.available"
                size="small"
                @change="(value) => changeQuantity(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">￥{{ subtotal(row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" @click="remove(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="cart-footer">
          <div class="cart-summary">
            已选 <strong>{{ selected.length }}</strong> 件
            <span class="total">合计 ￥{{ selectedTotal }}</span>
          </div>
          <div class="cart-actions">
            <el-button :disabled="!selected.length" :loading="favLoading" @click="toFavorite">转收藏</el-button>
            <el-button type="primary" :disabled="!selected.length" :loading="checkoutLoading" @click="checkout">去下单</el-button>
          </div>
        </div>
      </template>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getCart, updateCart, removeCart, cartToFavorite, cartCheckout } from '@/api/customer'

const router = useRouter()
const items = ref([])
const selected = ref([])
const loading = ref(false)
const favLoading = ref(false)
const checkoutLoading = ref(false)

const selectedTotal = computed(() => {
  const sum = selected.value.reduce((acc, item) => acc + Number(item.price || 0) * (item.quantity || 0), 0)
  return sum.toFixed(2)
})

async function load() {
  loading.value = true
  try {
    items.value = (await getCart()) || []
    selected.value = []
  } catch {
    items.value = []
    selected.value = []
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  selected.value = rows
}

function subtotal(row) {
  return (Number(row.price || 0) * (row.quantity || 0)).toFixed(2)
}

async function changeQuantity(row, value) {
  try {
    await updateCart(row.id, value)
  } catch {
    await load()
  }
}

async function remove(row) {
  try {
    await removeCart(row.id)
    ElMessage.success('已移除')
    await load()
  } catch {}
}

async function toFavorite() {
  if (!selected.value.length) return
  favLoading.value = true
  try {
    await cartToFavorite(selected.value.map((item) => item.id))
    ElMessage.success('已转为收藏')
    await load()
  } catch {
  } finally {
    favLoading.value = false
  }
}

async function checkout() {
  if (!selected.value.length) return
  checkoutLoading.value = true
  try {
    const order = await cartCheckout(selected.value.map((item) => item.id))
    ElMessage.success(`下单成功，订单号：${order.orderNo}`)
    await load()
    router.push('/customer/orders')
  } catch {
  } finally {
    checkoutLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.cart-container {
  max-width: 980px;
  margin: 0 auto;
}

.cart-heading {
  text-align: center;
  padding: 10px 0 24px;
}

.cart-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.state {
  min-height: 300px;
}

.cart-product {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-cover {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f5f3;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: #82928c;
  font-size: 11px;
}

.cart-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.cart-meta h3 {
  margin: 0;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e1e9e6;
  border-radius: 12px;
  flex-wrap: wrap;
}

.cart-summary {
  color: #61736e;
}

.cart-summary strong {
  color: #203b38;
}

.total {
  margin-left: 18px;
  color: #168675;
  font-size: 20px;
  font-weight: 800;
}

.cart-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 640px) {
  .cart-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .cart-actions {
    justify-content: flex-end;
  }
}
</style>