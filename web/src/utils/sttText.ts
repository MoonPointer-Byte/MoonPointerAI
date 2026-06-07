import { endsWithSentenceTerminator } from './sentenceBoundary'

/** 去除 SenseVoice 输出的 <|tag|> 与常见表情符号 */
export function cleanSenseVoiceText(raw: string): string {
  return raw
    .replace(/<\|[^|]+\|>/g, '')
    .replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/gu, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** 将相邻 STT 块拼接为连续文本（处理重叠与中日韩无空格） */
export function stitchSttChunks(carry: string, chunk: string): string {
  const a = carry.trim()
  const b = chunk.trim()
  if (!a) return b
  if (!b) return a
  if (b.startsWith(a)) return b
  if (a.endsWith(b)) return a

  const max = Math.min(a.length, b.length)
  for (let i = max; i > 3; i--) {
    if (a.slice(-i) === b.slice(0, i)) {
      return a + b.slice(i)
    }
  }

  const cjkTail = /[\u3040-\u30ff\u3400-\u9fff]$/.test(a)
  const cjkHead = /^[\u3040-\u30ff\u3400-\u9fff]/.test(b)
  if (cjkTail && cjkHead) return a + b

  return `${a} ${b}`.trim()
}

const JP_HOLD_SUFFIX = /[はがをにのでとへもやかねよなて]$/
const EN_HOLD_SUFFIX = /\b(a|an|the|is|are|was|were|to|of|and|but|or|if|when|that|this|it|he|she|they|we|you|i)$/i

/** 块边界上是否应继续等待下一块（避免半句话被定稿） */
export function shouldHoldForContinuation(text: string): boolean {
  const t = text.trim()
  if (!t || endsWithSentenceTerminator(t)) return false
  if (t.length >= 28) return false
  if (/[\u3040-\u30ff\u3400-\u9fff]/.test(t) && JP_HOLD_SUFFIX.test(t)) return true
  if (/[a-zA-Z]/.test(t) && EN_HOLD_SUFFIX.test(t)) return true
  return t.length < 8
}
