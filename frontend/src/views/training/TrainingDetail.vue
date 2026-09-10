<script setup lang="ts">
import { useRoute } from 'vue-router'

// 实训详情
const route = useRoute()
const id = route.params.id
</script>

<template>
  <el-card>
    <h2>实训详情 #{{ id }}</h2>
    <p>展示 AI 校验报告、评分明细、教师评语</p>
  </el-card>
</template>
