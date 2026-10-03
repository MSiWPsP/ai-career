/** 图表渲染在 Canvas 中，需将全局主题变量解析成实际色值。 */
export function getChartTheme() {
  const style = getComputedStyle(document.documentElement)
  const read = (name: string) => style.getPropertyValue(name).trim()
  return {
    primary: read('--primary'),
    text: read('--text'),
    muted: read('--muted'),
    line: read('--line'),
    surface: read('--surface'),
    subtle: read('--surface-subtle'),
  }
}
