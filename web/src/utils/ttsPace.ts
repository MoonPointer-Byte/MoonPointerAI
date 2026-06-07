/** 目标语 TTS 在 rate=1 时约每秒字符数（用于估算播报时长） */
export const BASE_TTS_CHARS_PER_SEC = 13

export interface SegmentPaceMetrics {
  sourceChars: number
  durationMs: number
  cps: number
}

export interface LiveSegmentPace {
  startMs: number
  maxChars: number
}

export function createLiveSegmentPace(now = Date.now()): LiveSegmentPace {
  return { startMs: now, maxChars: 0 }
}

export function updateLiveSegmentPace(pace: LiveSegmentPace, sourceText: string) {
  pace.maxChars = Math.max(pace.maxChars, sourceText.trim().length)
  return pace
}

export function finalizeSegmentPace(
  pace: LiveSegmentPace,
  sourceText: string,
  now = Date.now()
): SegmentPaceMetrics {
  const sourceChars = Math.max(pace.maxChars, sourceText.trim().length)
  const durationMs = Math.max(450, now - pace.startMs)
  const durationSec = durationMs / 1000
  const cps = sourceChars > 0 ? sourceChars / durationSec : 5
  return { sourceChars, durationMs, cps }
}

/**
 * 根据音源段时长与译文长度计算 speechSynthesis.rate，
 * 使 TTS 播报耗时尽量接近说话耗时。
 */
export function calcMatchedSpeechRate(
  translationText: string,
  metrics: SegmentPaceMetrics | undefined,
  fallbackCps: number,
  backlog = 0
): number {
  const MIN_RATE = 0.78
  const MAX_RATE = 1.55
  const chars = translationText.trim().length
  if (chars === 0) return 1

  const targetSec = metrics
    ? Math.max(0.45, metrics.durationMs / 1000)
    : Math.max(0.6, chars / Math.max(3, fallbackCps * 0.9))

  const baseDurationSec = chars / BASE_TTS_CHARS_PER_SEC
  let rate = baseDurationSec / targetSec

  if (metrics?.cps) {
    const cpsRate = metrics.cps / BASE_TTS_CHARS_PER_SEC
    rate = rate * 0.72 + cpsRate * 0.28
  }

  if (backlog > 1) {
    rate += Math.min(0.12, (backlog - 1) * 0.05)
  }

  return Math.min(MAX_RATE, Math.max(MIN_RATE, rate))
}

/** 默认：较快但清晰的固定语速 */
export function calcFastSpeechRate(backlog = 0): number {
  const FAST_RATE = 2.00
  if (backlog <= 1) return FAST_RATE
  return Math.min(1.62, FAST_RATE + Math.min(0.12, (backlog - 1) * 0.05))
}

export function calcSpeechRate(
  mode: 'fast' | 'match',
  translationText: string,
  metrics: SegmentPaceMetrics | undefined,
  fallbackCps: number,
  backlog = 0
): number {
  if (mode === 'fast') return calcFastSpeechRate(backlog)
  return calcMatchedSpeechRate(translationText, metrics, fallbackCps, backlog)
}
