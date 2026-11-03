<template>
  <BuyerLayout>
    <div class="home-container">
      <section class="hero">
        <p class="hero-kicker">ONLINE SHOP</p>
        <h1>发现值得入手的好物</h1>
        <p class="hero-sub">浏览在售商品，按类别或关键词查找，提交购买意向即可排队。</p>
      </section>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索商品名称或描述"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        >
          <template #append>
            <el-button @click="search">搜索</el-button>
          </template>
        </el-input>
        <el-cascader
          v-model="filters.categoryId"
          :options="categoryOptions"
          :props="cascaderProps"
          placeholder="全部类别"
          clearable
          class="category-cascader"
          @change="search"
        />
      </div>

      <div v-if="loading" class="list-state">
        <el-skeleton :rows="5" animated />
      </div>

      <el-empty
        v-else-if="!products.length"
        description="暂无在售商品"
      />

      <div v-else class="product-grid">
        <el-card
          v-for="item in products"
          :key="item.id"
          class="product-card"
          shadow="hover"
          @click="goDetail(item.id)"
        >
          <div class="cover">
            <img v-if="item.image" :src="item.image" :alt="item.name" />
            <div v-else class="cover-placeholder">暂无图片</div>
          </div>
          <div class="meta">
            <h3>{{ item.name }}</h3>
            <p class="price">￥{{ item.price }}</p>
            <div class="meta-row">
              <el-tag :type="item.status === 'FROZEN' ? 'warning' : 'success'" effect="light" size="small">
                {{ item.statusText }}
              </el-tag>
              <span v-if="item.status === 'ON_SALE'" class="stock">库存 {{ item.stock }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="total > filters.size" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="filters.size"
          :current-page="filters.page"
          @current-change="changePage"
        />
      </div>
    </div>
  </BuyerLayout>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BuyerLayout from '@/components/BuyerLayout.vue'
import { listProducts, getCategoryTree } from '@/api/product'

const router = useRouter()
const products = ref([])
const total = ref(0)
const loading = ref(false)
const categoryOptions = ref([])

const filters = reactive({ keyword: '', categoryId: null, page: 1, size: 12 })

const cascaderProps = {
  checkStrictly: true,
  emitPath: false,
  value: 'id',
  label: 'name',
  children: 'children',
}

async function load() {
  loading.value = true
  try {
    const data = await listProducts({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId ?? undefined,
      page: filters.page,
      size: filters.size,
    })
    products.value = data.records || []
    total.value = data.total || 0
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function changePage(page) {
  filters.page = page
  load()
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

onMounted(async () => {
  load()
  try {
    categoryOptions.value = (await getCategoryTree()) || []
  } catch {
    categoryOptions.value = []
  }
})
</script>

<style scoped>
.home-container {
  max-width: 1040px;
  margin: 0 auto;
}

.hero {
  text-align: center;
  padding: 18px 0 26px;
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
  font-size: clamp(26px, 5vw, 36px);
  letter-spacing: 1px;
}

.hero-sub {
  margin: 0 auto;
  max-width: 560px;
  color: #71817c;
  line-height: 1.7;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  flex: 1;
}

.category-cascader {
  width: 220px;
}

.list-state {
  min-height: 300px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.product-card {
  cursor: pointer;
  overflow: hidden;
}

.product-card :deep(.el-card__body) {
  padding: 0;
}

.cover {
  aspect-ratio: 1;
  background: #f0f5f3;
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
}

.meta {
  padding: 14px 16px 16px;
}

.meta h3 {
  margin: 0 0 8px;
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  margin: 0 0 10px;
  color: #168675;
  font-size: 22px;
  font-weight: 800;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stock {
  color: #8a9894;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
  }

  .category-cascader {
    width: 100%;
  }
}
</style>