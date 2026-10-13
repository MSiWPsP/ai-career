import type { CareerTask } from '../types/api'

/** 按规划顺序聚焦最早尚未结束的阶段；跨阶段进行中的任务仍优先续做。 */
export function currentStageTasks(tasks: CareerTask[], stages: string[]): CareerTask[] {
  const active = tasks.find(task => task.status === 1)
  const stageName = (task: CareerTask) => task.stageName || '未分组'
  const waitingStage = stages.find(name => tasks.some(task => stageName(task) === name && task.status < 2))
  const waiting = tasks.find(task => task.status < 2)
  const stage = active ? stageName(active) : waitingStage ?? (waiting ? stageName(waiting) : undefined)
  if (stage === undefined) return []
  return tasks.filter(task => stageName(task) === stage).sort((a, b) => {
    const weight = (status: number) => status === 1 ? 0 : status === 0 ? 1 : 2
    return weight(a.status) - weight(b.status) || b.priority - a.priority
  })
}

export function nextActionTask(tasks: CareerTask[], today: string): CareerTask | undefined {
  return tasks.find(task => task.status === 1)
    ?? tasks.find(task => task.status === 0 && (!task.startDate || task.startDate.slice(0, 10) <= today))
}
