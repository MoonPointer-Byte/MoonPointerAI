import { useCallback, useEffect, useRef, useState } from 'react'
import { randomId } from '../utils/randomId'

interface SpeechRecognitionEvent {
  resultIndex: number
  results: SpeechRecognitionResultList
}

interface SpeechRecognitionResultList {
  length: number
  [index: number]: SpeechRecognitionResult
}

interface SpeechRecognitionResult {
  isFinal: boolean
  [index: number]: { transcript: string }
}

interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean
  interimResults: boolean
  lang: string
  start(): void
  stop(): void
  abort(): void
  onresult: ((event: SpeechRecognitionEvent) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
  onstart: (() => void) | null
}

declare global {
  interface Window {
    SpeechRecognition: new () => SpeechRecognitionInstance
    webkitSpeechRecognition: new () => SpeechRecognitionInstance
  }
}

const RESTART_DELAY_MS = 200
const MAX_RESTART_DELAY_MS = 2000

export function useSpeechRecognition(
  lang: string,
  onResult: (text: string, isFinal: boolean, segmentId: string) => void,
  enabled: boolean
) {
  const [supported, setSupported] = useState(false)
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null)
  const segmentIdRef = useRef(randomId())
  const onResultRef = useRef(onResult)
  const enabledRef = useRef(enabled)
  const langRef = useRef(lang)
  const restartTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const restartAttemptsRef = useRef(0)
  const intentionalStopRef = useRef(false)
  const watchdogRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const listeningRef = useRef(false)

  onResultRef.current = onResult
  enabledRef.current = enabled
  langRef.current = lang

  const clearRestartTimer = () => {
    if (restartTimerRef.current) {
      clearTimeout(restartTimerRef.current)
      restartTimerRef.current = null
    }
  }

  const scheduleRestart = useCallback((delay = RESTART_DELAY_MS) => {
    clearRestartTimer()
    if (!enabledRef.current || intentionalStopRef.current) return

    restartTimerRef.current = setTimeout(() => {
      const rec = recognitionRef.current
      if (!rec || !enabledRef.current || intentionalStopRef.current) return
      try {
        rec.start()
        restartAttemptsRef.current = 0
        setListening(true)
        setError(null)
      } catch {
        restartAttemptsRef.current += 1
        const next = Math.min(
          MAX_RESTART_DELAY_MS,
          RESTART_DELAY_MS * Math.pow(2, restartAttemptsRef.current)
        )
        scheduleRestart(next)
      }
    }, delay)
  }, [])

  const createRecognition = useCallback(() => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) return null

    const recognition = new SR()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = langRef.current

    recognition.onstart = () => {
      listeningRef.current = true
      setListening(true)
      setError(null)
      restartAttemptsRef.current = 0
    }

    recognition.onresult = (event) => {
      let interim = ''
      let final = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        const transcript = result[0].transcript
        if (result.isFinal) {
          final += transcript
        } else {
          interim += transcript
        }
      }
      if (final) {
        onResultRef.current(final.trim(), true, segmentIdRef.current)
        segmentIdRef.current = randomId()
      } else if (interim) {
        onResultRef.current(interim.trim(), false, segmentIdRef.current)
      }
    }

    recognition.onerror = (event) => {
      const code = event.error
      if (code === 'aborted') return

      if (code === 'no-speech' || code === 'network') {
        scheduleRestart(300)
        return
      }

      const messages: Record<string, string> = {
        'not-allowed': '麦克风权限被拒绝，请在浏览器设置中允许麦克风',
        'service-not-allowed': '当前浏览器不支持语音识别，请用 Chrome 或 Edge 打开',
        'audio-capture': '未检测到麦克风设备',
      }
      if (messages[code]) {
        setError(messages[code])
        setListening(false)
        return
      }
      scheduleRestart(500)
    }

    recognition.onend = () => {
      listeningRef.current = false
      setListening(false)
      if (enabledRef.current && !intentionalStopRef.current) {
        scheduleRestart(RESTART_DELAY_MS)
      }
    }

    return recognition
  }, [scheduleRestart])

  const start = useCallback(() => {
    intentionalStopRef.current = false
    setError(null)

    if (!recognitionRef.current) {
      recognitionRef.current = createRecognition()
    }
    if (!recognitionRef.current) return

    recognitionRef.current.lang = langRef.current
    clearRestartTimer()
    try {
      recognitionRef.current.start()
      setListening(true)
    } catch {
      scheduleRestart(RESTART_DELAY_MS)
    }
  }, [createRecognition, scheduleRestart])

  const stop = useCallback(() => {
    intentionalStopRef.current = true
    clearRestartTimer()
    try {
      recognitionRef.current?.abort()
    } catch {
      recognitionRef.current?.stop()
    }
    listeningRef.current = false
    setListening(false)
  }, [])

  useEffect(() => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    setSupported(!!SR)
    if (!SR) return

    recognitionRef.current = createRecognition()

    return () => {
      intentionalStopRef.current = true
      clearRestartTimer()
      try {
        recognitionRef.current?.abort()
      } catch { /* ignore */ }
      recognitionRef.current = null
    }
  }, [createRecognition])

  useEffect(() => {
    if (recognitionRef.current) {
      recognitionRef.current.lang = lang
    }
  }, [lang])

  useEffect(() => {
    if (enabled) {
      start()
      watchdogRef.current = setInterval(() => {
        if (enabledRef.current && !intentionalStopRef.current && !listeningRef.current) {
          scheduleRestart(100)
        }
      }, 3000)
    } else {
      stop()
      if (watchdogRef.current) {
        clearInterval(watchdogRef.current)
        watchdogRef.current = null
      }
    }
    return () => {
      if (watchdogRef.current) {
        clearInterval(watchdogRef.current)
        watchdogRef.current = null
      }
    }
  }, [enabled, start, stop, scheduleRestart])

  return { supported, listening, error, start, stop }
}
