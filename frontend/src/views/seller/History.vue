<template>
  <SellerLayout
    ><div class="page-heading">
      <div>
        <h1>历史商品</h1>
        <div class="muted">按成交时间倒序查看商品及购买意向结果。</div>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <section class="panel" v-loading="loading">
      <el-alert
        v-if="error"
        :title="error"
        :type="unavailable ? 'info' : 'error'"
        :closable="false"
        show-icon
      />
      <el-table v-else :data="records" stripe empty-text="暂无历史商品"
        ><el-table-column
          prop="name"
          label="商品名称"
          min-width="180"
        /><el-table-column label="价格" min-width="120"
          ><template #default="{ row }"
            >￥{{ Number(row.price).toFixed(2) }}</template
          ></el-table-column
        ><el-table-column
          prop="createTime"
          label="发布时间"
          min-width="180"
        /><el-table-column
          prop="finishTime"
          label="成交时间"
          min-width="180"
        /><el-table-column label="操作" width="120"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row)"
              >查看明细</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <el-pagination
        v-if="!error && total"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="load"
      />
    </section>
    <el-dialog v-model="visible" title="历史商品明细" width="min(850px, 94vw)"
      ><template v-if="selected"
        ><h2>{{ selected.name }}</h2>
        <p class="description">{{ selected.description || "暂无描述" }}</p>
        <p>成交价：￥{{ Number(selected.price).toFixed(2) }}</p>
        <p class="muted">
          成交时间：{{ selected.finishTime || "—" }} · 历史记录只读
        </p>
        <h3>意向人及交易结果</h3>
        <div v-loading="detailLoading">
          <el-alert
            v-if="detailError"
            :title="detailError"
            type="warning"
            :closable="false"
          /><IntentTable v-else :items="intents" /></div></template
    ></el-dialog>
  </SellerLayout>
</template>
<script setup>
import { ref, onMounted } from "vue";
import SellerLayout from "@/components/SellerLayout.vue";
import IntentTable from "@/components/IntentTable.vue";
import { getHistory, getProductIntents } from "@/api/sellerWorkspace";
const records = ref([]),
  page = ref(1),
  size = 10,
  total = ref(0),
  loading = ref(false),
  error = ref(""),
  unavailable = ref(false);
const visible = ref(false),
  selected = ref(null),
  intents = ref([]),
  detailLoading = ref(false),
  detailError = ref("");
let request = 0,
  detailRequest = 0;
async function load() {
  const id = ++request;
  loading.value = true;
  error.value = "";
  try {
    const result = await getHistory(page.value, size);
    if (id !== request) return;
    records.value = result.records;
    total.value = result.total;
  } catch (err) {
    if (id !== request) return;
    unavailable.value = [404, 405].includes(err.response?.status);
    error.value = unavailable.value
      ? "历史查询暂不可用，等待服务接通后即可查看历史记录。"
      : "历史商品加载失败，请点击刷新重试。";
  } finally {
    if (id === request) loading.value = false;
  }
}
async function showDetail(row) {
  const id = ++detailRequest;
  selected.value = row;
  visible.value = true;
  intents.value = [];
  detailError.value = "";
  detailLoading.value = true;
  try {
    const result = await getProductIntents(row.id);
    if (id === detailRequest) intents.value = result;
  } catch (err) {
    if (id === detailRequest)
      detailError.value = [404, 405].includes(err.response?.status)
        ? "意向人明细查询暂不可用。"
        : "明细加载失败，请关闭后重新打开。";
  } finally {
    if (id === detailRequest) detailLoading.value = false;
  }
}
onMounted(load);
</script>
