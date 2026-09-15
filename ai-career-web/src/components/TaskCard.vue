<script setup lang="ts">
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'
import type { CareerTask } from '../types/api'
import { formatDate } from '../utils/data'

const props = defineProps<{ task: CareerTask; updating?: boolean }>()
const emit = defineEmits<{ changeStatus: [task: CareerTask, status: number] }>()

const priority = computed(() => {
  const map: Record<number, { label: string; type: 'danger' | 'warning' | 'info' }> = {
    3: { label: '高优先级', type: 'danger' },
    2: { label: '中优先级', type: 'warning' },
    1: { label: '低优先级', type: 'info' },
  }
  return map[props.task.priority] || map[2]
})
</script>

<template>
  <article class="task-card" :class="{ completed: task.status === 2 }">
    <button
      class="task-check"
      :aria-label="task.status === 2 ? '标记为进行中' : '标记为完成'"
      @click="emit('changeStatus', task, task.status === 2 ? 1 : 2)"
    >
      <el-icon v-if="task.status === 2"><Check /></el-icon>
    </button>
    <div class="task-main">
      <div class="task-title-row">
        <h3>{{ task.taskName }}</h3>
        <el-tag :type="priority.type" effect="light" size="small">{{ priority.label }}</el-tag>
      </div>
      <p>{{ task.taskDescription || '完成本项成长任务，持续接近你的职业目标。' }}</p>
      <div class="task-meta">
        <span>{{ task.stageName }}</span>
        <span>截止 {{ formatDate(task.deadline) }}</span>
      </div>
    </div>
    <el-button
      :loading="updating"
      :type="task.status === 2 ? 'success' : task.status === 1 ? 'primary' : 'default'"
      plain
      @click="emit('changeStatus', task, task.status === 0 ? 1 : 2)"
    >
      {{ task.status === 2 ? '已完成' : task.status === 1 ? '完成任务' : '开始任务' }}
    </el-button>
  </article>
</template>

<style scoped>
.task-card {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  align-items: start;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.task-card:last-child {
  border-bottom: 0;
}

.task-check {
  display: grid;
  width: 22px;
  height: 22px;
  margin-top: 1px;
  place-items: center;
  border: 2px solid #c4d1df;
  border-radius: 7px;
  color: #fff;
  background: #fff;
  cursor: pointer;
}

.completed .task-check {
  border-color: var(--success);
  background: var(--success);
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-title-row h3 {
  margin: 0;
  font-size: 15px;
}

.completed h3 {
  color: var(--tertiary);
  text-decoration: line-through;
}

.task-main p {
  margin: 7px 0 10px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.task-meta {
  display: flex;
  gap: 16px;
  color: var(--tertiary);
  font-size: 12px;
}

@media (max-width: 680px) {
  .task-card {
    grid-template-columns: 24px 1fr;
  }

  .task-card > .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
