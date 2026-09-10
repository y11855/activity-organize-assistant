<script setup lang="ts">
import { ref } from 'vue'
import type { EvaluationMetric } from '@/types'

const metrics = ref<EvaluationMetric[]>([
  { id: 1, name: '步骤完整性', description: '关键步骤是否齐全', weight: 0.4, scoreType: 'ai' },
  { id: 2, name: '合规性', description: '是否符合实训要求', weight: 0.3, scoreType: 'ai' },
  { id: 3, name: '代码规范', description: '代码风格', weight: 0.15, scoreType: 'teacher' },
  { id: 4, name: '创新性', description: '亮点加分', weight: 0.15, scoreType: 'teacher' }
])
</script>

<template>
  <el-card>
    <template #header>
      <span>评价指标管理</span>
      <el-button type="primary" style="float: right">新增指标</el-button>
    </template>
    <el-table :data="metrics" border>
      <el-table-column prop="name" label="指标名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="weight" label="权重" width="100" />
      <el-table-column label="评分方式" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.scoreType === 'ai' ? 'AI' : '教师' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
