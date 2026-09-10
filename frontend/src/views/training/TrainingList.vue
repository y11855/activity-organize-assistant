<script setup lang="ts">
import { ref } from 'vue'

// 实训列表
const list = ref<{ id: number; title: string; status: number }[]>([])
</script>

<template>
  <el-card>
    <template #header>
      <div class="training-list__header">
        <span>实训管理</span>
        <el-button type="primary">上传实训成果</el-button>
      </div>
    </template>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="实训标题" />
      <el-table-column prop="status" label="状态" width="120" />
    </el-table>
  </el-card>
</template>

<style scoped lang="scss">
.training-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
