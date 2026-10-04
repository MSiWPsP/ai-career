<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { RadarChart } from 'echarts/charts'
import { LegendComponent, RadarComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { AbilityRadar } from '../types/api'
import { getChartTheme } from '../utils/chartTheme'

use([RadarChart, RadarComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = withDefaults(
  defineProps<{
    data: AbilityRadar
    height?: number
    name?: string
    emptyDescription?: string
  }>(),
  { height: 300, name: '当前能力', emptyDescription: '完成模拟面试并生成报告后，这里会展示被考查维度的 AI 评估结果。' },
)

const chartRef = ref<HTMLDivElement>()
let chart: ECharts | undefined
let observer: ResizeObserver | undefined
let resizeFrame: number | undefined

const compactAbilities = computed(() => props.data.indicators.map((indicator, index) => {
  const value = props.data.values[index] ?? 0
  const maximum = indicator.max || 100
  return {
    name: indicator.name,
    value,
    percentage: Math.max(0, Math.min(100, Math.round(value / maximum * 100))),
  }
}))

const useCompactBars = computed(() => compactAbilities.value.length > 0 && compactAbilities.value.length < 3)

function render() {
  if (!chartRef.value || props.data.indicators.length === 0) return
  chart ||= init(chartRef.value)
  const theme = getChartTheme()
  const option: EChartsCoreOption = {
    color: [theme.primary],
    tooltip: { backgroundColor: theme.surface, borderColor: theme.line, textStyle: { color: theme.text } },
    radar: {
      radius: '66%',
      splitNumber: 4,
      indicator: props.data.indicators,
      axisName: { color: theme.muted, fontSize: 12 },
      splitArea: { areaStyle: { color: [theme.surface, theme.subtle] } },
      splitLine: { lineStyle: { color: theme.line } },
      axisLine: { lineStyle: { color: theme.line } },
    },
    series: [
      {
        type: 'radar',
        data: [{ name: props.name, value: props.data.values }],
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { color: theme.primary, opacity: 0.18 },
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
  <div
    v-if="useCompactBars"
    class="compact-abilities"
    :style="{ height: height + 'px' }"
  >
    <div class="compact-list">
      <div v-for="ability in compactAbilities" :key="ability.name" class="compact-item">
        <div class="compact-heading">
          <strong>{{ ability.name }}</strong>
          <span>{{ ability.value }} 分</span>
        </div>
        <div class="compact-track">
          <span :style="{ width: ability.percentage + '%' }" />
        </div>
      </div>
      <small>能力维度达到 3 项后，将自动切换为雷达图展示</small>
    </div>
  </div>
  <div
    v-else-if="data.indicators.length"
    ref="chartRef"
    class="radar-chart"
    :style="{ height: height + 'px' }"
  />
  <div v-else class="chart-empty">
    <el-icon class="empty-icon"><DataAnalysis /></el-icon>
    <strong>还没有能力数据</strong>
    <small>{{ emptyDescription }}</small>
  </div>
</template>

<style scoped>
.radar-chart {
  width: 100%;
}

.compact-abilities {
  display: grid;
  width: 100%;
  place-items: center;
}

.compact-list {
  display: grid;
  width: min(100%, 430px);
  gap: 22px;
}

.compact-list > small {
  color: var(--muted);
  text-align: center;
}

.compact-item {
  display: grid;
  gap: 9px;
}

.compact-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.compact-heading strong {
  overflow: hidden;
  color: var(--text);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-heading span {
  flex: none;
  color: var(--primary);
  font-weight: 700;
}

.compact-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--line);
}

.compact-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5969ec, #4c5cf1);
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
