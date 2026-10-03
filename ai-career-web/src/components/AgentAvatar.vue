<script setup lang="ts">
import { computed } from 'vue'
import careerPlannerAvatar from '../assets/career-planner-avatar.jpg'
import interviewerAvatar from '../assets/interviewer-avatar.jpg'

const props = withDefaults(defineProps<{
  role: 'planner' | 'interviewer'
  size?: number
}>(), {
  size: 40,
})

const source = computed(() => props.role === 'planner' ? careerPlannerAvatar : interviewerAvatar)
const alternativeText = computed(() => props.role === 'planner' ? 'AI 职业规划师头像' : 'AI 模拟面试官头像')
</script>

<template>
  <span
    class="agent-avatar"
    :class="`agent-avatar--${role}`"
    :style="{ '--agent-avatar-size': `${size}px` }"
  >
    <img :src="source" :alt="alternativeText" />
  </span>
</template>

<style scoped>
.agent-avatar {
  display: block;
  width: var(--agent-avatar-size);
  height: var(--agent-avatar-size);
  flex: 0 0 var(--agent-avatar-size);
  overflow: hidden;
  border: 1px solid #bfd2ee;
  border-radius: 12px;
  background: #eaf2ff;
  box-shadow: 0 3px 10px rgb(23 49 92 / 10%);
}

.agent-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.agent-avatar--planner img {
  transform: scale(1.45) translateY(7%);
}

.agent-avatar--interviewer {
  border-radius: 50%;
}

.agent-avatar--interviewer img {
  transform: scale(1.28) translateY(5%);
}
</style>
