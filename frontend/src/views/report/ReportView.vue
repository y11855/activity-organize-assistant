<script setup lang="ts">
import { ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { downloadBlob } from '@/utils'

echarts.use([BarChart, GridComponent, TooltipComponent, TitleComponent, CanvasRenderer])

const option = ref({
  title: { text: '成绩分布' },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['0-59', '60-69', '70-79', '80-89', '90-100']
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data: [5, 12, 28, 35, 18]
    }
  ]
})

function exportExcel() {
  // TODO: 使用 xlsx 生成
  downloadBlob(new Blob(['占位']), 'report.xlsx')
}

function exportPdf() {
  // TODO: 使用 jspdf + html2canvas 生成
}
</script>

<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between">
        <span>统计报表</span>
        <div>
          <el-button type="primary" @click="exportExcel">导出 Excel</el-button>
          <el-button @click="exportPdf">导出 PDF</el-button>
        </div>
      </div>
    </template>
    <v-chart :option="option" style="height: 400px" />
  </el-card>
</template>
