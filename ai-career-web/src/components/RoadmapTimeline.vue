<script setup lang="ts">
import { TrendCharts } from '@element-plus/icons-vue'
import type { RoadmapStage } from '../types/api'

defineProps<{ stages: RoadmapStage[] }>()
</script>

<template>
  <div v-if="stages.length" class="roadmap">
    <article v-for="(stage, index) in stages" :key="stage.stage || index" class="roadmap-stage">
      <div class="stage-marker">
        <span>{{ stage.stage || index + 1 }}</span>
        <i v-if="index < stages.length - 1" />
      </div>
      <div class="stage-content">
        <div class="stage-top">
          <h3>{{ stage.name || '成长阶段 ' + (index + 1) }}</h3>
          <span>{{ stage.duration || '持续推进' }}</span>
        </div>
        <p>{{ stage.goal || '围绕目标岗位逐步完善核心能力。' }}</p>
        <div v-if="stage.topics?.length" class="stage-topics">
          <span v-for="topic in stage.topics" :key="topic">{{ topic }}</span>
        </div>
      </div>
    </article>
  </div>
  <div v-else class="empty-panel">
    <div>
      <el-icon class="empty-icon"><TrendCharts /></el-icon>
      <strong>成长路线等待生成</strong>
      <span>完善职业画像后，AI 将为你拆解阶段性成长目标。</span>
    </div>
  </div>
</template>

<style scoped>
.roadmap-stage {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 14px;
}

.stage-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stage-marker span {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--primary);
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 5px 14px rgba(29, 78, 216, 0.18);
}

.stage-marker i {
  width: 2px;
  min-height: 78px;
  flex: 1;
  background: linear-gradient(#60a5fa, #dbeafe);
}

.stage-content {
  margin-bottom: 14px;
  padding: 16px 18px;
  border: 1px solid #d9e6f5;
  border-radius: 11px;
  background: linear-gradient(145deg, #fff, #f7fbff);
  box-shadow: 0 7px 20px rgb(23 49 92 / 5%);
  transition: border-color var(--motion-fast) ease, box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard);
}

.stage-content:hover { border-color: #9fc3f2; box-shadow: 0 15px 30px rgb(29 78 216 / 11%); transform: translateX(5px); }

.stage-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stage-top h3 {
  margin: 0;
  font-size: 15px;
}

.stage-top > span {
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.stage-content p {
  margin: 8px 0 10px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.stage-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.stage-topics span {
  padding: 5px 9px;
  border-radius: 7px;
  border: 1px solid #dfe8f3;
  color: var(--primary);
  background: var(--primary-soft);
  font-size: 11px;
}
</style>
