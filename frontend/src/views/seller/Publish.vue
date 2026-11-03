<template>
  <SellerLayout
    ><div class="page-heading">
      <div>
        <h1>发布商品</h1>
        <div class="muted">支持多商品同时在售，发布时填写库存并选择二级类别。</div>
      </div>
    </div>
    <div class="publish-grid">
      <section class="panel" v-loading="loading">
        <el-form
          novalidate
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <el-form-item label="商品名称" prop="name"
            ><el-input
              v-model="form.name"
              maxlength="100"
              show-word-limit
              placeholder="例如：九成新台灯"
          /></el-form-item>
          <el-form-item label="商品描述（选填）" prop="description"
            ><el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="介绍成色、规格及交易方式"
          /></el-form-item>
          <el-form-item label="商品图片（最多 9 张，第一张为封面）">
            <input type="file" accept="image/jpeg,image/png" multiple :disabled="uploading || loading" aria-label="上传商品图片" @change="upload" />
            <p>JPG / PNG，单张不超过 5MB。按上传顺序展示。</p>
            <div v-for="(url,index) in images" :key="url" style="display:flex;align-items:center;gap:8px;margin:8px"><el-image :src="url" style="width:72px;height:72px" fit="cover"/><el-button :disabled="index===0" @click="moveImage(index)">前移</el-button><el-button :disabled="loading || uploading" @click="images.splice(index,1)">移除</el-button></div>
          </el-form-item>
          <el-form-item label="售价（元）" prop="price"
            ><el-input-number
              v-model="form.price"
              :min="0.01"
              :max="99999999.99"
              :precision="2"
              :step="1"
              :controls="false"
          /></el-form-item>
          <el-form-item label="库存数量"><el-input-number v-model="form.stock" :min="1" :max="99999" :precision="0" /></el-form-item>
          <el-form-item label="二级类别（选填）"><el-select v-model="form.categoryId" clearable placeholder="未分类"><el-option-group v-for="root in categories" :key="root.id" :label="root.name"><el-option v-for="child in root.children" :key="child.id" :label="child.name" :value="child.id" /></el-option-group></el-select></el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="loading || uploading"
            >确认发布</el-button
          ><el-button @click="$router.push('/seller/admin')"
            >返回后台</el-button
          >
        </el-form>
      </section>
      <section class="panel">
        <h2>商品预览</h2>
        <el-image
          v-if="images.length"
          class="product-image"
          :src="images[0]"
          fit="cover"
          ><template #error>图片加载失败</template></el-image
        >
        <h3>{{ form.name.trim() || "商品名称" }}</h3>
        <p class="price">
          ￥{{ form.price ? Number(form.price).toFixed(2) : "0.00" }}
        </p>
        <p class="description">
          {{ form.description || "商品描述将显示在这里" }}
        </p>
        <p class="muted">
          发布后不支持修改，请核对名称、图片、库存及售价。
        </p>

      </section>
    </div>
  </SellerLayout>
</template>
<script setup>
import { reactive, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import SellerLayout from "@/components/SellerLayout.vue";
import http from "@/utils/http";
import { publishProduct } from "@/api/product";
import { getCategories } from "@/api/sellerWorkspace";
const router = useRouter(),
  formRef = ref(),
  loading = ref(false),
  categories = ref([]);
const images = ref([]), uploading = ref(false);
const form = reactive({
  name: "",
  description: "",
  price: undefined,
  stock: 1,
  categoryId: undefined,
});
const rules = {
  name: [
    {
      required: true,
      whitespace: true,
      message: "请输入商品名称",
      trigger: "blur",
    },
    { max: 100, message: "名称最多 100 个字符", trigger: "blur" },
  ],
  price: [
    {
      validator: (_, value, cb) =>
        cb(
          typeof value !== "number" ||
            !Number.isFinite(value) ||
            value < 0.01 ||
            value > 99999999.99
            ? new Error("请输入 0.01～99999999.99 元的价格")
            : undefined,
        ),
      trigger: "change",
    },
  ],

};
function moveImage(index) { [images.value[index-1],images.value[index]] = [images.value[index],images.value[index-1]]; }
async function upload(event) {
  const files=[...event.target.files];event.target.value='';
  if(files.length+images.value.length>9){ElMessage.warning('最多 9 张图片');return;}
  if(files.some(file=>!['image/jpeg','image/png'].includes(file.type)||file.size>5*1024*1024)){ElMessage.warning('请选择不超过 5MB 的 JPG / PNG 图片');return;}
  uploading.value=true;
  try { for(const file of files){const data=new FormData();data.append('file',file);const result=await http.post('/product/image',data);images.value.push(result.url);} } catch {} finally {uploading.value=false;}
}
async function submit() {
  if (
    loading.value || uploading.value ||
    !(await formRef.value.validate().catch(() => false))
  )
    return;
  if (!Number.isInteger(form.stock) || form.stock < 1 || form.stock > 99999) { ElMessage.warning("库存需为 1～99999 的整数"); return; }
  loading.value = true;
  try {
    await publishProduct({
      ...form,
      name: form.name.trim(),
      images: [...images.value],
      categoryId: form.categoryId || undefined,
      description: form.description.trim(),
    });
    ElMessage.success("商品发布成功");
    await router.push("/seller/products");
  } catch {

  } finally {
    loading.value = false;
  }
}
onMounted(async () => { try { categories.value = await getCategories(); } catch { ElMessage.error("类别加载失败，请刷新重试"); } });
</script>
