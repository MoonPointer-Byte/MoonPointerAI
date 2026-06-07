const PREFERRED_NAME_PATTERNS = [
  /neural/i,
  /natural/i,
  /premium/i,
  /online/i,
  /google/i,
  /xiaoxiao/i,
  /yunxi/i,
  /tingting/i,
  /meijia/i,
  /huihui/i,
  /kangkang/i,
]

export function resolveTtsLang(tag: string): string {
  const t = tag.trim()
  if (!t) return 'zh-CN'
  if (t.includes('-')) return t
  const map: Record<string, string> = {
    zh: 'zh-CN',
    en: 'en-US',
    ja: 'ja-JP',
    ko: 'ko-KR',
  }
  return map[t] ?? t
}

function scoreVoice(voice: SpeechSynthesisVoice, lang: string): number {
  const langBase = lang.split('-')[0].toLowerCase()
  const vLang = voice.lang.toLowerCase().replace('_', '-')
  const target = lang.toLowerCase()
  let score = 0
  if (vLang === target) score += 100
  else if (vLang.startsWith(langBase)) score += 60
  else return -1

  if (voice.localService) score += 25
  for (let i = 0; i < PREFERRED_NAME_PATTERNS.length; i++) {
    if (PREFERRED_NAME_PATTERNS[i].test(voice.name)) {
      score += 30 - i
    }
  }
  if (/microsoft/i.test(voice.name)) score += 8
  return score
}

/** 优先本地语音，避免云端语音未下载导致无声 */
export function pickBestVoice(lang: string): SpeechSynthesisVoice | null {
  if (typeof window === 'undefined' || !window.speechSynthesis) return null
  const resolved = resolveTtsLang(lang)
  const voices = window.speechSynthesis.getVoices()
  if (!voices.length) return null

  let best: SpeechSynthesisVoice | null = null
  let bestScore = -1
  for (const v of voices) {
    const s = scoreVoice(v, resolved)
    if (s > bestScore) {
      bestScore = s
      best = v
    }
  }

  if (best && !best.localService) {
    const local = voices.find((v) => v.localService && scoreVoice(v, resolved) > 0)
    if (local) return local
  }

  return best
}

export function warmUpVoices(): void {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  const synth = window.speechSynthesis
  const load = () => synth.getVoices()
  load()
  synth.addEventListener('voiceschanged', load)
}
