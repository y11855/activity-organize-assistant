<script setup lang="ts">
// 403
</script>

<template>
  <el-result icon="error" title="403" sub-title="无权限访问该页面，请联系管理员分配权限">
    <template #extra>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </template>
  </el-result>
</template>
