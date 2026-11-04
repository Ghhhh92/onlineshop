<template>
<div class="rich-editor">
  <div class="toolbar"><el-button @click="format('bold')">加粗</el-button><el-button @click="format('insertUnorderedList')">列表</el-button><label>插入图片<input type="file" accept="image/jpeg,image/png" :disabled="busy" @change="insertImage" /></label><el-button @click="insertVideo">插入视频</el-button></div>
  <div ref="editor" contenteditable="true" role="textbox" aria-label="商品富文本描述" aria-multiline="true" class="editor-body" @input="sync" @paste.prevent="paste" />
  <p class="muted">可输入文字、插入图片及 HTTP/HTTPS 视频。图片为 JPG/PNG，单张不超过 5MB。</p>
</div>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/utils/http';
import { cleanDescription } from '@/utils/richText';
const emit=defineEmits(['update:modelValue','busy']);
defineProps({modelValue:String});
const editor=ref(),busy=ref(false);
function sync(){emit('update:modelValue',cleanDescription(editor.value.innerHTML));}
function format(command){editor.value.focus();document.execCommand(command);sync();}
function paste(event){document.execCommand('insertText',false,event.clipboardData.getData('text/plain'));sync();}
async function insertImage(event){const file=event.target.files[0];event.target.value='';if(!file)return;if(!['image/jpeg','image/png'].includes(file.type)||file.size>5*1024*1024){ElMessage.warning('请选择不超过 5MB 的 JPG/PNG 图片');return;}busy.value=true;emit('busy',true);try{const data=new FormData();data.append('file',file);const {url}=await http.post('/product/image',data);const image=document.createElement('img');image.src=url;image.alt='描述图片';editor.value.append(image);sync();}catch{}finally{busy.value=false;emit('busy',false);}}
async function insertVideo(){try{const {value}=await ElMessageBox.prompt('输入可直接播放的 HTTP/HTTPS 视频地址（如 MP4）','插入视频',{inputValidator:v=>{try{return ['http:','https:'].includes(new URL(v).protocol)||'请输入 HTTP/HTTPS 地址';}catch{return '请输入有效的视频地址';}}});const video=document.createElement('video');video.src=value;video.controls=true;editor.value.append(video);sync();}catch{}}
</script>
<style scoped>
.rich-editor{width:100%}.toolbar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}.editor-body{min-height:180px;border:1px solid #dcdfe6;border-radius:6px;padding:12px;outline-color:#168675}.editor-body :deep(img),.editor-body :deep(video){max-width:100%}
</style>
