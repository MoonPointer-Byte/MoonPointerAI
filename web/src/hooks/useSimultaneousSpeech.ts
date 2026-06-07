import { useCallback, useRef } from 'react'
import type { WsMessage } from '../types'

/** 尽快把识别文本送到翻译（流式 interim） */
const INTERIM_DELAY_MS = 0
const MIN_CHARS = 1
const GROWTH_CHARS = 1

export function useSimultaneousSpeech(
  send: (msg: WsMessage) => void,
  sourceLang: string,
  targetLang: string
) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastSentRef = useRef('')
  const pendingRef = useRef('')
  const segmentIdRef = useRef('')

  const sendInterim = useCallback(
    (segmentId: string) => {
      const stable = pendingRef.current.trim()
      if (stable.length < MIN_CHARS || stable === lastSentRef.current) return
      lastSentRef.current = stable
      send({
        type: 'SPEECH',
        segmentId,
        text: stable,
        isFinal: false,
        sourceLang,
        targetLang,
      })
    },
    [send, sourceLang, targetLang]
  )

  const sendSpeech = useCallback(
    (text: string, isFinal: boolean, segmentId: string) => {
      const trimmed = text.trim()
      if (!trimmed) return

      if (isFinal) {
        if (timerRef.current) clearTimeout(timerRef.current)
        const sameAsInterim = trimmed === lastSentRef.current
        lastSentRef.current = ''
        pendingRef.current = ''
        segmentIdRef.current = ''
        if (sameAsInterim) {
          send({
            type: 'SPEECH',
            segmentId,
            text: trimmed,
            isFinal: true,
            sourceLang,
            targetLang,
          })
          return
        }
        send({
          type: 'SPEECH',
          segmentId,
          text: trimmed,
          isFinal: true,
          sourceLang,
          targetLang,
        })
        return
      }

      if (segmentIdRef.current !== segmentId) {
        segmentIdRef.current = segmentId
        lastSentRef.current = ''
      }

      pendingRef.current = trimmed
      if (trimmed.length < MIN_CHARS) return

      const growth = trimmed.length - lastSentRef.current.length
      if (lastSentRef.current === '' || growth >= GROWTH_CHARS) {
        if (timerRef.current) clearTimeout(timerRef.current)
        sendInterim(segmentId)
        return
      }

      if (timerRef.current) clearTimeout(timerRef.current)
      timerRef.current = setTimeout(() => sendInterim(segmentId), INTERIM_DELAY_MS)
    },
    [send, sourceLang, targetLang, sendInterim]
  )

  const reset = useCallback(() => {
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = null
    lastSentRef.current = ''
    pendingRef.current = ''
    segmentIdRef.current = ''
  }, [])

  return { sendSpeech, reset }
}
