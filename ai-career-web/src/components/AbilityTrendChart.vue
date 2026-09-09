<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { AbilityTrend } from '../types/api'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = withDefaults(defineProps<{ data: AbilityTrend; height?: number }>(), { height: 290 })
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | undefined
let observer: ResizeObserver | undefined

function render() {
  if (!chartRef.value || !props.data.records.length) return
  chart ||= init(chartRef.value)
  const option: EChartsCoreOption = {
    color: ['#5b5edc'],
    tooltip: { trigger: 'axis' },
    grid: { left: 38, right: 18, top: 25, bottom: 35 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.records.map((item) => item.date),
      axisLine: { lineStyle: { color: '#dfe1ea' } },
      axisTick: { show: false },
      axisLabel: { color: '#8b8fa2', fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: '#ececf3', type: 'dashed' } },
      axisLabel: { color: '#8b8fa2', fontSize: 10 },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: props.data.records.map((item) => item.score),
        lineStyle: { width: 3 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(91, 94, 220, 0.28)' },
              { offset: 1, color: 'rgba(91, 94, 220, 0.02)' },
            ],
          },
        },
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
  <div v-if="data.records.length" ref="chartRef" class="trend-chart" :style="{ height: height + 'px' }" />
  <div v-else class="chart-empty">
    <span class="empty-icon">⌁</span>
    <strong>还没有趋势数据</strong>
    <small>同一项能力产生多次评分后，会形成清晰的成长曲线。</small>
  </div>
</template>

<style scoped>
.trend-chart { width: 100%; }
.chart-empty { display: grid; min-height: 270px; place-content: center; color: var(--muted); text-align: center; }
.chart-empty strong,
.chart-empty small { display: block; }
.chart-empty strong { color: var(--text); }
.chart-empty small { max-width: 310px; margin-top: 7px; line-height: 1.6; }
</style>
