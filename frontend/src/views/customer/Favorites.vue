<template>
  <BuyerLayout>
    <div class="favorites-container">
      <section class="favorites-heading">
        <h1>我的收藏</h1>
        <p class="buyer-muted">收藏的商品会保留在这里，下架商品会标注「已下架/无货」。</p>
      </section>

      <div v-if="loading" class="state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty v-else-if="!items.length" description="还没有收藏商品" />

      <div v-else class="favorite-list">
        <el-card v-for="item in items" :key="item.id" class="favorite-item">
          <div class="favorite-row">
            <div class="cover">
              <img v-if="item.image" :src="item.image" :alt="item.productName" />
              <div v-else class="cover-placeholder">暂无图片</div>
            </div>
            <div class="meta">
              <h3>{{ item.productName || '商品已不存在' }}</h3>
              <p class="price">￥{{ item.price }}</p>
              <p class="muted-line">
                <el-tag :type="item.available ? 'success' : 'info'" size="small" effect="light">
                  {{ item.delisted ? '已下架/无货' : (item.status === 'ON_SALE' ? '在售' : (item.status || '未知')) }}
                </el-tag>
                <span v-if="item.available" class="stock">库存 {{ item.stock }}</span>
              </p>
            </div>
            <div class="actions">
              <el-button size="small" type="primary" :disabled="!item.available" @click="addToCart(item)">加入购物车</el-button>
              <el-button size="small" @click="remove(item)">取消收藏</el-button>
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
import { ElMessage } from 'element-plus'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { getFavorites, removeFavorite, addCart } from '@/api/customer'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getFavorites({ page: page.value, size })
    items.value = data.records || []
    total.value = data.total || 0
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

async function remove(item) {
  try {
    await removeFavorite(item.productId)
    ElMessage.success('已取消收藏')
    await load()
  } catch {}
}

async function addToCart(item) {
  try {
    await addCart(item.productId, 1)
    ElMessage.success('已加入购物车')
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.favorites-container {
  max-width: 760px;
  margin: 0 auto;
}

.favorites-heading {
  text-align: center;
  padding: 10px 0 24px;
}

.favorites-heading h1 {
  margin: 0 0 10px;
  font-size: 30px;
}

.state {
  min-height: 300px;
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.favorite-item :deep(.el-card__body) {
  padding: 16px 18px;
}

.favorite-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover {
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

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: #82928c;
  font-size: 12px;
}

.meta {
  flex: 1;
  min-width: 0;
}

.meta h3 {
  margin: 0 0 6px;
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  margin: 0 0 8px;
  color: #168675;
  font-weight: 700;
}

.muted-line {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.stock {
  color: #8a9894;
  font-size: 13px;
}

.actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .favorite-row {
    flex-wrap: wrap;
  }

  .actions {
    flex-direction: row;
    width: 100%;
  }
}
</style>