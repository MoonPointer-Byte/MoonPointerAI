import { useCallback, useRef } from 'react'
import {
  endsWithSentenceTerminator,
  splitIntoSentences,
} from '../utils/sentenceBoundary'
import { shouldHoldForContinuation, stitchSttChunks } from '../utils/sttText'
import { randomId } from '../utils/randomId'
import type { SentenceEvent } from './useSentenceSegmenter'

/**
 * 耳机模式专用：按 STT 音频块分句，而非浏览器流式识别。
 * 每块到达即出 live 预览；有标点则按句定稿，否则在块边界定稿（过短未说完则暂挂）。
 */
export function useChunkSentenceSegmenter() {
  const carryRef = useRef('')
  const liveSegmentIdRef = useRef(randomId())

  const reset = useCallback(() => {
    carryRef.current = ''
    liveSegmentIdRef.current = randomId()
  }, [])

  const newLiveId = () => {
    liveSegmentIdRef.current = randomId()
    return liveSegmentIdRef.current
  }

  const processChunk = useCallback((chunk: string): SentenceEvent[] => {
    const incoming = chunk.trim()
    if (!incoming) return []

    const merged = stitchSttChunks(carryRef.current, incoming)
    const events: SentenceEvent[] = []

    events.push({
      text: merged,
      segmentId: liveSegmentIdRef.current,
      isComplete: false,
    })

    const parts = splitIntoSentences(merged)
    const complete: string[] = []
    let remainder = ''

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isLast = i === parts.length - 1
      if (!isLast || endsWithSentenceTerminator(part)) {
        complete.push(part)
      } else {
        remainder = part
      }
    }

    if (complete.length > 0) {
      for (const text of complete) {
        events.push({ text, segmentId: newLiveId(), isComplete: true })
      }
      carryRef.current = remainder
      if (remainder) {
        liveSegmentIdRef.current = randomId()
        events.push({
          text: remainder,
          segmentId: liveSegmentIdRef.current,
          isComplete: false,
        })
      } else {
        newLiveId()
      }
      return events
    }

    if (shouldHoldForContinuation(merged)) {
      carryRef.current = merged
      return events
    }

    const finalizeId = liveSegmentIdRef.current
    events.push({ text: merged, segmentId: finalizeId, isComplete: true })
    carryRef.current = ''
    newLiveId()
    return events
  }, [])

  const flush = useCallback((): SentenceEvent | null => {
    const pending = carryRef.current.trim()
    if (!pending) return null
    carryRef.current = ''
    const event: SentenceEvent = {
      text: pending,
      segmentId: liveSegmentIdRef.current,
      isComplete: true,
    }
    newLiveId()
    return event
  }, [])

  return { processChunk, flush, reset }
}
