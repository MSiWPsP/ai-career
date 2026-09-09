<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { RadarChart } from 'echarts/charts'
import { LegendComponent, RadarComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { AbilityRadar } from '../types/api'

use([RadarChart, RadarComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = withDefaults(
  defineProps<{
    data: AbilityRadar
    height?: number
    name?: string
  }>(),
  { height: 300, name: '当前能力' },
)

const chartRef = ref<HTMLDivElement>()
let chart: ECharts | undefined
let observer: ResizeObserver | undefined

function render() {
  if (!chartRef.value || props.data.indicators.length === 0) return
  chart ||= init(chartRef.value)
  const option: EChartsCoreOption = {
    color: ['#5b5edc'],
    tooltip: {},
    radar: {
      radius: '66%',
      splitNumber: 4,
      indicator: props.data.indicators,
      axisName: { color: '#555a70', fontSize: 12 },
      splitArea: { areaStyle: { color: ['#fbfbff', '#f5f6ff'] } },
      splitLine: { lineStyle: { color: '#e2e4f2' } },
      axisLine: { lineStyle: { color: '#d9dcec' } },
    },
    series: [
      {
        type: 'radar',
        data: [{ name: props.name, value: props.data.values }],
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(91, 94, 220, 0.2)' },
      },
    ],
  }
  chart.setOption(option, true)
}

watch(() => props.data, render, { deep: true })

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  if (chartRef.value) observer.observe(chartRef.value)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div
    v-if="data.indicators.length"
    ref="chartRef"
    class="radar-chart"
    :style="{ height: height + 'px' }"
  />
  <div v-else class="chart-empty">
    <el-icon class="empty-icon"><DataAnalysis /></el-icon>
    <strong>还没有能力数据</strong>
    <small>完成技能画像或模拟面试后，这里会形成你的能力图谱。</small>
  </div>
</template>

<style scoped>
.radar-chart {
  width: 100%;
}

.chart-empty {
  display: grid;
  min-height: 260px;
  place-content: center;
  color: var(--muted);
  text-align: center;
}

.chart-empty strong,
.chart-empty small {
  display: block;
}

.chart-empty strong {
  color: var(--text);
}

.chart-empty small {
  max-width: 310px;
  margin-top: 7px;
  line-height: 1.6;
}
</style>
