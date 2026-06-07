import { useCallback, useEffect, useRef } from 'react'
import {
  calcSpeechRate,
  createLiveSegmentPace,
  finalizeSegmentPace,
  type LiveSegmentPace,
  type SegmentPaceMetrics,
  updateLiveSegmentPace,
} from '../utils/ttsPace'
import { setTtsQueueResetHandler, speakTts, stopAllTts } from '../utils/ttsEngine'
import { warmUpVoices } from '../utils/ttsVoice'

interface QueueItem {
  segmentId: string
  text: string
}

function normalizeTtsText(text: string): string {
  return text.trim().replace(/\s+/g, '')
}

function isWorthReSpeaking(prev: string | undefined, next: string): boolean {
  if (!prev) return true
  const a = normalizeTtsText(prev)
  const b = normalizeTtsText(next)
  if (!a || !b) return a !== b
  if (a === b) return false
  if (a.includes(b) || b.includes(a)) {
    return Math.abs(a.length - b.length) > 3
  }
  return true
}

export function useSimultaneousTts(
  enabled: boolean,
  lang = 'zh-CN',
  matchSource = false
) {
  const queueRef = useRef<QueueItem[]>([])
  const playingRef = useRef(false)
  const enabledRef = useRef(enabled)
  const segmentOrderRef = useRef(0)
  const segmentRankRef = useRef<Map<string, number>>(new Map())
  const spokenRef = useRef<Map<string, string>>(new Map())
  const livePaceRef = useRef<Map<string, LiveSegmentPace>>(new Map())
  const segmentMetricsRef = useRef<Map<string, SegmentPaceMetrics>>(new Map())
  const globalCpsRef = useRef(5)
  const matchSourceRef = useRef(matchSource)
  const langRef = useRef(lang)
  const playHeadRef = useRef<() => void>(() => {})

  enabledRef.current = enabled
  matchSourceRef.current = matchSource
  langRef.current = lang

  const resetQueueState = useCallback(() => {
    queueRef.current = []
    playingRef.current = false
  }, [])

  const clearQueue = useCallback(() => {
    stopAllTts()
    resetQueueState()
    segmentRankRef.current.clear()
    segmentOrderRef.current = 0
    spokenRef.current.clear()
    livePaceRef.current.clear()
    segmentMetricsRef.current.clear()
  }, [resetQueueState])

  const ensureRank = useCallback((segmentId: string) => {
    if (!segmentRankRef.current.has(segmentId)) {
      segmentRankRef.current.set(segmentId, segmentOrderRef.current++)
    }
    return segmentRankRef.current.get(segmentId)!
  }, [])

  const rateFor = useCallback((segmentId: string, text: string) => {
    const mode = matchSourceRef.current ? 'match' : 'fast'
    const metrics = segmentMetricsRef.current.get(segmentId)
    return calcSpeechRate(
      mode,
      text,
      metrics,
      globalCpsRef.current,
      queueRef.current.length
    )
  }, [])

  const playHead = useCallback(() => {
    if (!enabledRef.current || playingRef.current || queueRef.current.length === 0) return

    queueRef.current.sort(
      (a, b) =>
        (segmentRankRef.current.get(a.segmentId) ?? 0) -
        (segmentRankRef.current.get(b.segmentId) ?? 0)
    )

    const head = queueRef.current[0]
    const text = head.text.trim()
    if (!text) {
      queueRef.current.shift()
      playHeadRef.current()
      return
    }

    playingRef.current = true
    const rate = rateFor(head.segmentId, text)

    const onDone = () => {
      playingRef.current = false
      spokenRef.current.set(head.segmentId, text)
      queueRef.current.shift()
      playHeadRef.current()
    }

    const started = speakTts(text, langRef.current, rate, onDone, onDone)
    if (!started) {
      playingRef.current = false
      queueRef.current.shift()
      playHeadRef.current()
    }
  }, [rateFor])

  playHeadRef.current = playHead

  const enqueueOnce = useCallback(
    (segmentId: string, text: string) => {
      if (!enabledRef.current) return
      const normalized = text.trim()
      if (!normalized) return

      if (spokenRef.current.get(segmentId) === normalized) return
      if (queueRef.current.some((q) => q.segmentId === segmentId && q.text === normalized)) return

      ensureRank(segmentId)
      queueRef.current = queueRef.current.filter((q) => q.segmentId !== segmentId)
      queueRef.current.push({ segmentId, text: normalized })
      playHeadRef.current()
    },
    [ensureRank]
  )

  const onStreamUpdate = useCallback(
    (segmentId: string, translation: string, isFinal: boolean) => {
      if (!enabledRef.current || !isFinal || !translation.trim()) return
      enqueueOnce(segmentId, translation)
    },
    [enqueueOnce]
  )

  const reportSourceActivity = useCallback(
    (sourceText: string, isFinal: boolean, segmentId: string) => {
      if (!enabledRef.current || !matchSourceRef.current) return
      ensureRank(segmentId)

      const trimmed = sourceText.trim()
      if (!trimmed) return

      const now = Date.now()
      let live = livePaceRef.current.get(segmentId)
      if (!live) {
        live = createLiveSegmentPace(now)
        livePaceRef.current.set(segmentId, live)
      }
      updateLiveSegmentPace(live, trimmed)

      if (!isFinal) return

      const metrics = finalizeSegmentPace(live, trimmed, now)
      segmentMetricsRef.current.set(segmentId, metrics)
      livePaceRef.current.delete(segmentId)
      globalCpsRef.current = globalCpsRef.current * 0.35 + metrics.cps * 0.65
    },
    [ensureRank]
  )

  const speakCorrection = useCallback(
    (translation: string, segmentId?: string) => {
      if (!enabledRef.current || !translation.trim()) return
      const id = segmentId?.trim()
      if (!id) return

      const normalized = translation.trim()
      const prev = spokenRef.current.get(id)
      if (!isWorthReSpeaking(prev, normalized)) return

      queueRef.current = queueRef.current.filter((q) => q.segmentId !== id)
      enqueueOnce(id, normalized)
    },
    [enqueueOnce]
  )

  const stop = useCallback(() => {
    clearQueue()
    globalCpsRef.current = 5
  }, [clearQueue])

  useEffect(() => {
    setTtsQueueResetHandler(resetQueueState)
    return () => setTtsQueueResetHandler(null)
  }, [resetQueueState])

  useEffect(() => {
    warmUpVoices()
  }, [lang])

  useEffect(() => {
    if (!enabled) stop()
  }, [enabled, stop])

  return {
    onStreamUpdate,
    reportSourceActivity,
    speakCorrection,
    stop,
  }
}
