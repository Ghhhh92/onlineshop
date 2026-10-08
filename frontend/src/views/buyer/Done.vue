<template>
  <div class="done-container">
    <el-card>
      <h2>提交成功</h2>
      <p>您的口令码：<strong style="font-size: 24px; color: #12b5a5">{{ code }}</strong></p>
      <p>请妥善保管口令码，用于查询排队状态。</p>
    </el-card>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const code = route.params.code
</script>

<style scoped>
.done-container {
  max-width: 600px;
  margin: 100px auto;
  text-align: center;
}
</style>
