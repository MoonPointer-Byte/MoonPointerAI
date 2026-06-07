/** 是否以句末标点结尾 */
export function endsWithSentenceTerminator(text: string): boolean {
  return /[.!?…。！？]["'”’)\]]*\s*$/.test(text.trim())
}

/** 将文本拆成若干句子（保留句末标点） */
export function splitIntoSentences(text: string): string[] {
  const trimmed = text.trim()
  if (!trimmed) return []

  const parts = trimmed.match(/[^.!?…。！？]+[.!?…。！？]+["'”’)\]]*|[^.!?…。！？]+$/g)
  if (!parts) return [trimmed]
  return parts.map((s) => s.trim()).filter(Boolean)
}

/** 停顿后是否应把缓冲内容作为一句定稿 */
export function shouldFlushOnPause(text: string): boolean {
  const t = text.trim()
  if (!t) return false
  if (endsWithSentenceTerminator(t)) return true
  // 说话人停顿且已有足够内容，视为一句（避免切得过碎）
  if (t.length >= 35) return true
  return false
}
