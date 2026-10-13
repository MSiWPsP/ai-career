/** 文案与目标状态共用一份定义；恢复跳过任务不等于完成任务。 */
export function taskAction(status: number): { label: string; target?: number } {
  return ({
    0: { label: '开始任务', target: 1 },
    1: { label: '标记完成', target: 2 },
    2: { label: '已完成' },
    3: { label: '恢复任务', target: 0 },
  } as Record<number, { label: string; target?: number }>)[status] ?? { label: '状态未知' }
}

export function taskCheckAction(status: number): { label: string; target?: number } {
  return status === 2 ? { label: '重新打开任务', target: 1 } : taskAction(status)
}
