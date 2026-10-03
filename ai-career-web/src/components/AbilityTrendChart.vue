<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { AbilityTrend } from '../types/api'
import { getChartTheme } from '../utils/chartTheme'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = withDefaults(defineProps<{ data: AbilityTrend; height?: number }>(), { height: 290 })
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | undefined
let observer: ResizeObserver | undefined
let resizeFrame: number | undefined

function render() {
  if (!chartRef.value || !props.data.records.length) return
  chart ||= init(chartRef.value)
  const theme = getChartTheme()
  const option: EChartsCoreOption = {
    color: [theme.primary],
    tooltip: { trigger: 'axis', backgroundColor: theme.surface, borderColor: theme.line, textStyle: { color: theme.text } },
    grid: { left: 38, right: 18, top: 25, bottom: 35 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.records.map((item) => item.date),
      axisLine: { lineStyle: { color: theme.line } },
      axisTick: { show: false },
      axisLabel: { color: theme.muted, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: theme.line, type: 'dashed' } },
      axisLabel: { color: theme.muted, fontSize: 11 },
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
              { offset: 0, color: 'rgba(29, 78, 216, 0.22)' },
              { offset: 1, color: 'rgba(29, 78, 216, 0.02)' },
            ],
          },
        },
      },
    ],
  }
  chart.setOption(option, true)
}

async function renderAfterDomUpdate() {
  await nextTick()
  render()
}

watch(() => props.data, renderAfterDomUpdate, { deep: true, flush: 'post' })

watch(chartRef, (element, previousElement) => {
  if (previousElement) observer?.unobserve(previousElement)
  if (!element) {
    chart?.dispose()
    chart = undefined
    return
  }
  observer?.observe(element)
  render()
}, { flush: 'post' })

onMounted(() => {
  observer = new ResizeObserver(() => {
    if (resizeFrame) cancelAnimationFrame(resizeFrame)
    resizeFrame = requestAnimationFrame(() => chart?.resize())
  })
  if (chartRef.value) observer.observe(chartRef.value)
  void renderAfterDomUpdate()
})
onBeforeUnmount(() => {
  if (resizeFrame) cancelAnimationFrame(resizeFrame)
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div v-if="data.records.length" ref="chartRef" class="trend-chart" :style="{ height: height + 'px' }" />
  <div v-else class="chart-empty">
    <el-icon class="empty-icon"><TrendCharts /></el-icon>
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
