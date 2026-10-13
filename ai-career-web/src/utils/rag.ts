import type { CareerKnowledgeReference } from '../types/api'

const SOURCE_MARKER = '\n\n<!-- ai-career-knowledge-references -->\n参考依据\n'
const SOURCE_MARKER_TOKEN = '<!-- ai-career-knowledge-references -->'
const SOURCE_LINE = /^\d+\. 《([^》\n]+)》(?:·\s*(.+))?$/

/** 兼容历史消息的文本尾注；旧消息没有独立签名，不能作为审计级来源证明。 */
export function splitCareerKnowledgeSources(content: string): {
  body: string
  references: CareerKnowledgeReference[]
} {
  const markerAt = content.lastIndexOf(SOURCE_MARKER)
  if (markerAt < 0) return { body: content, references: [] }

  const lines = content.slice(markerAt + SOURCE_MARKER.length).trim().split('\n')
  const references: CareerKnowledgeReference[] = []
  for (const line of lines) {
    const match = SOURCE_LINE.exec(line.trim())
    if (!match) return { body: content, references: [] }
    references.push({
      documentId: '',
      title: match[1] ?? '',
      section: match[2]?.trim() ?? '',
      sourceName: '',
    })
  }
  if (!references.length) return { body: content, references: [] }
  return { body: content.slice(0, markerAt), references }
}

/** 实时模型文本不是可信引用元数据；仅服务端 done 事件能决定来源卡片。 */
export function removeUntrustedCareerKnowledgeMarker(content: string): string {
  return content.replaceAll(SOURCE_MARKER_TOKEN, '')
}
