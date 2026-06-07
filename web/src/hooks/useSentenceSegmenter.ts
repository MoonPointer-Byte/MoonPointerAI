import { useCallback, useRef } from 'react'
import {
  endsWithSentenceTerminator,
  shouldFlushOnPause,
  splitIntoSentences,
} from '../utils/sentenceBoundary'
import { randomId } from '../utils/randomId'

export interface SentenceEvent {
  text: string
  segmentId: string
  /** true = 整句已定稿，应请求精度翻译并写入字幕列表 */
  isComplete: boolean
}

export function useSentenceSegmenter() {
  const bufferRef = useRef('')
  const liveSegmentIdRef = useRef(randomId())

  const reset = useCallback(() => {
    bufferRef.current = ''
    liveSegmentIdRef.current = randomId()
  }, [])

  const newLiveId = () => {
    liveSegmentIdRef.current = randomId()
    return liveSegmentIdRef.current
  }

  const processChunk = useCallback((chunk: string, isFinal: boolean): SentenceEvent[] => {
    const trimmed = chunk.trim()
    if (!trimmed) return []

    if (!isFinal) {
      const display = bufferRef.current
        ? `${bufferRef.current} ${trimmed}`.trim()
        : trimmed
      return [{ text: display, segmentId: liveSegmentIdRef.current, isComplete: false }]
    }

    const merged = bufferRef.current
      ? `${bufferRef.current} ${trimmed}`.trim()
      : trimmed

    const events: SentenceEvent[] = []
    const parts = splitIntoSentences(merged)
    let remainder = ''

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isLast = i === parts.length - 1

      if (!isLast || endsWithSentenceTerminator(part)) {
        events.push({
          text: part,
          segmentId: liveSegmentIdRef.current,
          isComplete: true,
        })
        newLiveId()
      } else {
        remainder = part
      }
    }

    if (!events.length && shouldFlushOnPause(merged)) {
      events.push({
        text: merged,
        segmentId: liveSegmentIdRef.current,
        isComplete: true,
      })
      newLiveId()
      remainder = ''
    } else if (!events.length) {
      bufferRef.current = merged
      events.push({
        text: merged,
        segmentId: liveSegmentIdRef.current,
        isComplete: false,
      })
      return events
    }

    bufferRef.current = remainder
    if (remainder) {
      newLiveId()
      events.push({
        text: remainder,
        segmentId: liveSegmentIdRef.current,
        isComplete: false,
      })
    } else {
      newLiveId()
    }

    return events
  }, [])

  const flush = useCallback((): SentenceEvent | null => {
    const pending = bufferRef.current.trim()
    if (!pending) return null
    bufferRef.current = ''
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
