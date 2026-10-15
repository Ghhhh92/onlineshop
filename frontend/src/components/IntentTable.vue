<template>
  <el-table :data="sorted" stripe style="width: 100%" empty-text="暂无购买意向">
    <el-table-column prop="position" label="位次" width="80" />
    <el-table-column prop="name" label="姓名" min-width="110" />
    <el-table-column prop="phone" label="联系电话" min-width="150" />
    <el-table-column prop="createTime" label="提交时间" min-width="180" />
    <el-table-column label="结果 / 状态" min-width="120"
      ><template #default="{ row }"
        ><el-tag :type="row.status === 'SUCCESS' ? 'success' : 'info'">{{
          statuses[row.status] || row.status || "—"
        }}</el-tag></template
      ></el-table-column
    >
  </el-table>
</template>
<script setup>
import { computed } from "vue";
const props = defineProps({ items: { type: Array, default: () => [] } });
const sorted = computed(() =>
  [...props.items].sort(
    (a, b) =>
      String(a.createTime).localeCompare(String(b.createTime)) || a.id - b.id,
  ),
);
const statuses = {
  QUEUING: "排队中",
  TRADING: "交易中",
  SUCCESS: "交易成功",
  FAILED: "交易失败",
  CANCELED: "已撤销",
};
</script>
