<template>
  <div class="tree-demo">
    <div class="demo-header">
      <h2>活动分解树</h2>
      <p class="demo-desc">
        以树形结构展示每个活动的完整构成：活动 → 任务分工 / 物料清单 / 报名问卷 / 定时提醒。
        点击节点可在右侧查看详情，数据来自后端 <code>/api/activities/breakdown-tree</code> 接口。
      </p>
    </div>
    <TreeView />
  </div>
</template>

<script setup lang="ts">
import TreeView from '@/components/TreeView.vue'
</script>

<style scoped>
.tree-demo {
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.demo-header {
  margin-bottom: 16px;
}
.demo-header h2 {
  margin: 0 0 6px;
  font-size: 20px;
}
.demo-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}
.demo-desc code {
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
</style>
