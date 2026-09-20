import type { CareerKnowledgeReference } from '../types/api'

const SOURCE_MARKER = '\n\n<!-- ai-career-knowledge-references -->\n参考依据\n'
const SOURCE_LINE = /^\d+\. 《([^》\n]+)》(?:·\s*(.+))?$/

/** 只识别服务端写入的标记；普通模型文本即使写了“参考依据”也不会被当作可信来源。 */
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
