<template><div class="rich-description" v-html="cleanDescription(value)" /></template>
<script setup>
import { cleanDescription } from '@/utils/richText';
defineProps({ value: { type: String, default: '' } });
</script>
<style>
.rich-description { overflow-wrap:anywhere; white-space:pre-wrap; }
.rich-description img,.rich-description video { max-width:100%; height:auto; }
</style>
