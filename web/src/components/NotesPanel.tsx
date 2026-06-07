import { useState } from 'react'
import type { AiStatus, NoteMode, NoteRecord } from '../hooks/useNotes'

interface NotesPanelProps {
  note: NoteRecord
  mode: NoteMode
  onModeChange: (mode: NoteMode) => void
  onChange: (patch: Partial<NoteRecord>) => void
  onGenerate: () => Promise<void>
  saving: boolean
  saved: boolean
  dbReady: boolean
  generating: boolean
  segmentCount: number
}

function formatTime(iso?: string) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

function statusLabel(aiStatus: AiStatus, generating: boolean) {
  if (generating || aiStatus === 'GENERATING') return 'AI 生成中…'
  if (aiStatus === 'FAILED') return '生成失败'
  if (aiStatus === 'DONE') return 'AI 已生成'
  return '待生成'
}

export function NotesPanel({
  note,
  mode,
  onModeChange,
  onChange,
  onGenerate,
  saving,
  saved,
  dbReady,
  generating,
  segmentCount,
}: NotesPanelProps) {
  const [genError, setGenError] = useState<string | null>(null)

  const handleGenerate = async () => {
    setGenError(null)
    if (segmentCount === 0) {
      setGenError('请先开始传译，积累字幕后再生成')
      return
    }
    try {
      await onGenerate()
    } catch (e) {
      setGenError(e instanceof Error ? e.message : '生成失败')
    }
  }

  return (
    <section className="panel notes-panel">
      <div className="panel-header">
        <h2>学习笔记</h2>
        <span className={`save-status ${saved ? 'saved' : ''}`}>
          {!dbReady ? '数据库未连接' : saving ? '保存中…' : saved ? '已保存' : '待保存'}
        </span>
      </div>

      <div className="notes-mode-tabs">
        <button
          type="button"
          className={`notes-mode-tab ${mode === 'ai' ? 'active' : ''}`}
          onClick={() => onModeChange('ai')}
        >
          <span className="tab-icon">✦</span>
          AI 记录
        </button>
        <button
          type="button"
          className={`notes-mode-tab ${mode === 'manual' ? 'active' : ''}`}
          onClick={() => onModeChange('manual')}
        >
          <span className="tab-icon">✎</span>
          手写记录
        </button>
      </div>

      <input
        className="notes-title-input"
        value={note.title}
        onChange={(e) => onChange({ title: e.target.value })}
        placeholder="笔记标题（可选）"
      />

      {mode === 'manual' ? (
        <textarea
          className="notes-editor"
          value={note.manualContent}
          onChange={(e) => onChange({ manualContent: e.target.value })}
          placeholder="记录关键词、句型、心得…&#10;内容自动保存到 MySQL"
        />
      ) : (
        <div className="notes-ai-area">
          <div className="notes-ai-toolbar">
            <span className="notes-ai-meta">
              已收录 {segmentCount} 条字幕
              {note.lastAiGeneratedAt && ` · 上次 ${formatTime(note.lastAiGeneratedAt)}`}
            </span>
            <button
              type="button"
              className="btn-generate"
              onClick={handleGenerate}
              disabled={generating || !dbReady}
            >
              {generating ? '生成中…' : note.aiContent ? '重新生成' : '生成 AI 笔记'}
            </button>
          </div>

          <div className={`notes-ai-status status-${note.aiStatus.toLowerCase()}`}>
            {statusLabel(note.aiStatus, generating)}
          </div>

          {genError && <div className="notes-ai-error">{genError}</div>}

          <div className="notes-ai-content">
            {note.aiContent ? (
              <pre className="notes-ai-preview">{note.aiContent}</pre>
            ) : (
              <div className="notes-ai-empty">
                <p>AI 将根据实时字幕自动生成学习笔记</p>
                <ul>
                  <li>内容概要</li>
                  <li>重点词汇</li>
                  <li>精彩句型</li>
                </ul>
                <p className="notes-ai-hint">开始传译后，点击「生成 AI 笔记」</p>
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  )
}
