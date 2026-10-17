<template>
  <div class="buy-container">
    <el-card>
      <template #header><h2 style="margin: 0">提交购买意向</h2></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" style="max-width: 500px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请填写姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请填写 11 位手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const form = reactive({ name: '', phone: '' })
const rules = {
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
}

async function submit() {
  await formRef.value.validate()
  // TODO: 待后端「提交意向」接口（李帆）就绪后：调用接口拿到口令码，再跳转 /done/:code
  ElMessage.success('提交成功（待后端提交意向接口对接）')
}
</script>

<style scoped>
.buy-container {
  max-width: 600px;
  margin: 60px auto;
  padding: 0 20px;
}
</style>