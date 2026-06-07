import { memo } from 'react'
import { formatElapsed } from '../utils/formatTime'
import { unlockWebTts } from '../utils/ttsEngine'

const PREFIX_RE = /^(翻译|译文|中文翻译|英文翻译|Translation|Translated|Output|注)\s*[:：]\s*/i
const CODE_BLOCK_RE = /```[\w]*\s*([\s\S]*?)```/

export function sanitizeTranslation(text: string): string {
  let t = text.trim()
  if (!t || t.startsWith('[待翻译]')) return ''

  const codeMatch = t.match(CODE_BLOCK_RE)
  if (codeMatch) t = codeMatch[1].trim()

  t = t.replace(/^```[\w]*\s*/, '').replace(/\s*```$/, '')

  const lines = t.split(/\r?\n/).filter((l) => l.trim())
  if (lines.length > 1) t = lines[0].trim()

  t = t.replace(PREFIX_RE, '')

  if (
    (t.startsWith('"') && t.endsWith('"')) ||
    (t.startsWith('「') && t.endsWith('」')) ||
    (t.startsWith('『') && t.endsWith('』'))
  ) {
    t = t.slice(1, -1).trim()
  }

  return t
}

function SpeakButton({
  label,
  onClick,
}: {
  label: string
  onClick: () => void
}) {
  const handleClick = () => {
    unlockWebTts()
    onClick()
  }

  return (
    <button
      type="button"
      className="line-speak-btn"
      onClick={handleClick}
      aria-label={label}
      title={label}
    >
      <span className="line-speak-icon" aria-hidden>🔊</span>
    </button>
  )
}

interface TranscriptBlockProps {
  source: string
  translation: string
  time?: number
  showSource: boolean
  showTranslation: boolean
  live?: boolean
  pending?: boolean
  corrected?: boolean
  onSpeakSource?: (text: string) => void
  onSpeakTranslation?: (text: string) => void
}

export const TranscriptBlock = memo(function TranscriptBlock({
  source,
  translation,
  time,
  showSource,
  showTranslation,
  live,
  pending,
  corrected,
  onSpeakSource,
  onSpeakTranslation,
}: TranscriptBlockProps) {
  if (!source && !translation) return null

  const canSpeakSource = !live && !!onSpeakSource
  const speakableTranslation =
    translation && translation !== '…' && translation !== '翻译中…' && translation !== '翻译失败'
  const canSpeakTranslation = !live && !pending && !!onSpeakTranslation && !!speakableTranslation

  return (
    <article
      className={`transcript-block ${live ? 'live' : ''} ${pending ? 'pending' : ''} ${corrected ? 'corrected' : ''}`}
    >
      <div className="transcript-body">
        {showSource && source && (
          <div className="transcript-line">
            <p className="transcript-source">{source}</p>
            {canSpeakSource && (
              <SpeakButton label="朗读原文" onClick={() => onSpeakSource!(source)} />
            )}
          </div>
        )}
        {showTranslation && speakableTranslation && (
          <div className="transcript-line">
            <p className="transcript-target">
              {translation}
              {live && <span className="live-cursor">|</span>}
            </p>
            {canSpeakTranslation && (
              <SpeakButton label="朗读译文" onClick={() => onSpeakTranslation!(translation)} />
            )}
          </div>
        )}
      </div>
      {time !== undefined && (
        <time className="transcript-time">{formatElapsed(time)}</time>
      )}
    </article>
  )
})
