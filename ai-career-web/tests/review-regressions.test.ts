import test from 'node:test'
import assert from 'node:assert/strict'
import { taskAction, taskCheckAction } from '../src/utils/taskActions.ts'
import { ApiRequestError, isNotFound } from '../src/utils/apiError.ts'
import { selfAssessmentRadar, skillsChanged } from '../src/utils/skillAssessment.ts'
import { currentStageTasks, nextActionTask } from '../src/utils/growthTasks.ts'

test('任务主按钮逐项匹配状态，跳过只恢复到待开始', () => {
  assert.deepEqual(taskAction(0), { label: '开始任务', target: 1 })
  assert.deepEqual(taskAction(1), { label: '标记完成', target: 2 })
  assert.deepEqual(taskAction(2), { label: '已完成' })
  assert.deepEqual(taskAction(3), { label: '恢复任务', target: 0 })
  assert.equal(taskCheckAction(0).target, 1)
  assert.equal(taskCheckAction(2).target, 1)
  assert.equal(taskCheckAction(3).target, 0)
  assert.equal(taskAction(99).target, undefined)
})

const saved = [
  { id: 1, skillName: 'Java', skillCategory: 'PROGRAMMING', level: 3, score: 60, source: 'SELF' },
  { id: 2, skillName: 'MySQL', skillCategory: 'DATABASE', level: 4, score: 72, source: 'INTERVIEW' },
]
test('技能全清零要发送空集合，未修改不得覆盖面试来源', () => {
  assert.equal(skillsChanged(saved, []), true)
  assert.equal(skillsChanged([], []), false)
  assert.equal(skillsChanged(saved, [...saved].reverse()), false)
  assert.equal(skillsChanged(saved, [{ ...saved[0]!, level: 4 }]), true)
})
test('自评初始画像不混入面试来源', () => {
  assert.deepEqual(selfAssessmentRadar(saved), { indicators: [{ name: 'Java', max: 100 }], values: [60] })
  assert.deepEqual(selfAssessmentRadar([]), { indicators: [], values: [] })
})
test('没有规划与请求故障分别识别，不根据异常文案猜测', () => {
  assert.equal(isNotFound(new ApiRequestError('不存在', 404)), true)
  assert.equal(isNotFound({ response: { status: 404 } }), true)
  assert.equal(isNotFound(new ApiRequestError('当前职业规划不存在', 500)), false)
  assert.equal(isNotFound(new Error('Network Error')), false)
})

const task = (id: string, stageName: string, status: number, priority = 2, startDate?: string) => ({
  id, careerPlanId: '1', taskName: id, taskType: 'KNOWLEDGE', stageName, status, priority, startDate,
})
test('首页不按接口前三条跨阶段推荐，未来任务不提前开始', () => {
  const tasks = [task('late', '项目', 0, 3), task('first', '基础', 0, 1), task('skip', '基础', 3)]
  assert.deepEqual(currentStageTasks(tasks, ['基础', '项目']).map(item => item.id), ['first', 'skip'])
  assert.equal(nextActionTask([task('future', '基础', 0, 2, '2026-10-10')], '2026-10-04'), undefined)
  assert.equal(nextActionTask(currentStageTasks(tasks, ['基础', '项目']), '2026-10-04')?.id, 'first')
  assert.deepEqual(currentStageTasks([task('done', '基础', 2)], ['基础']), [])
})
test('跨阶段已开始的任务优先续做，已跳过不能成为下一步', () => {
  const tasks = [task('first', '基础', 0), task('active', '项目', 1), task('skip', '项目', 3)]
  assert.equal(nextActionTask(currentStageTasks(tasks, ['基础', '项目']), '2026-10-04')?.id, 'active')
  assert.equal(nextActionTask([task('skip', '基础', 3)], '2026-10-04'), undefined)
})
