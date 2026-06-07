import { useCallback, useEffect, useRef, useState } from 'react'
import { cleanSenseVoiceText } from '../utils/sttText'
import { whisperLangCode } from '../utils/whisperLang'

const CHUNK_MS = 3000
const MAX_QUEUE = 8
const AUDIO_BITS_PER_SECOND = 128_000

function getCaptureStream(video: HTMLVideoElement): MediaStream | null {
  const cap = (video as HTMLVideoElement & { captureStream?: () => MediaStream }).captureStream
    ?? (video as HTMLVideoElement & { mozCaptureStream?: () => MediaStream }).mozCaptureStream
  if (!cap) return null
  try {
    const stream = cap.call(video)
    const tracks = stream.getAudioTracks()
    if (tracks.length === 0) return null
    return new MediaStream(tracks)
  } catch {
    return null
  }
}

export function useTabAudioSpeech(
  serverUrl: string,
  sourceLang: string,
  onResult: (text: string, isFinal: boolean) => void,
  enabled: boolean,
  getVideoElement: () => HTMLVideoElement | null
) {
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [capturing, setCapturing] = useState(false)

  const recorderRef = useRef<MediaRecorder | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const chunkTimerRef = useRef<number | null>(null)
  const busyRef = useRef(false)
  const queueRef = useRef<Blob[]>([])
  const fatalRef = useRef(false)
  const consecutiveErrorsRef = useRef(0)
  const enabledRef = useRef(enabled)
  const onResultRef = useRef(onResult)
  const intentionalStopRef = useRef(false)
  const stopRef = useRef<() => void>(() => {})

  enabledRef.current = enabled
  onResultRef.current = onResult

  const apiBase = serverUrl.replace(/\/$/, '')

  const clearChunkTimer = useCallback(() => {
    if (chunkTimerRef.current != null) {
      window.clearTimeout(chunkTimerRef.current)
      chunkTimerRef.current = null
    }
  }, [])

  const stopStream = useCallback(() => {
    clearChunkTimer()
    queueRef.current = []
    recorderRef.current = null
    streamRef.current?.getTracks().forEach((t) => t.stop())
    streamRef.current = null
    setListening(false)
    setCapturing(false)
  }, [clearChunkTimer])

  const isFatalSttError = (msg: string) =>
    /余额|balance|403|401|invalid.*key|未配置/i.test(msg)

  const pumpQueue = useCallback(async () => {
    if (busyRef.current || fatalRef.current || !enabledRef.current) return
    const blob = queueRef.current.shift()
    if (!blob) return

    busyRef.current = true
    try {
      const form = new FormData()
      form.append('file', blob, 'chunk.webm')
      form.append('language', whisperLangCode(sourceLang))
      const res = await fetch(`${apiBase}/api/stt/transcribe`, {
        method: 'POST',
        body: form,
      })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        throw new Error(data.message || `语音识别失败 (${res.status})`)
      }
      const raw = cleanSenseVoiceText((data.text as string) ?? '')
      if (raw) {
        consecutiveErrorsRef.current = 0
        onResultRef.current(raw, true)
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : '语音识别失败'
      consecutiveErrorsRef.current += 1
      if (enabledRef.current) {
        setError(msg)
        if (isFatalSttError(msg) || consecutiveErrorsRef.current >= 5) {
          fatalRef.current = true
          stopRef.current()
        }
      }
    } finally {
      busyRef.current = false
      if (queueRef.current.length > 0) {
        void pumpQueue()
      }
    }
  }, [apiBase, sourceLang])

  const enqueueTranscribe = useCallback(
    (blob: Blob) => {
      if (blob.size < 800 || fatalRef.current || !enabledRef.current) return
      queueRef.current.push(blob)
      if (queueRef.current.length > MAX_QUEUE) {
        queueRef.current.shift()
      }
      void pumpQueue()
    },
    [pumpQueue]
  )

  const recordNextChunk = useCallback(() => {
    const stream = streamRef.current
    if (
      !stream
      || intentionalStopRef.current
      || fatalRef.current
      || !enabledRef.current
    ) {
      return
    }

    const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : MediaRecorder.isTypeSupported('audio/webm')
        ? 'audio/webm'
        : ''

    const recorder = mime
      ? new MediaRecorder(stream, { mimeType: mime, audioBitsPerSecond: AUDIO_BITS_PER_SECOND })
      : new MediaRecorder(stream, { audioBitsPerSecond: AUDIO_BITS_PER_SECOND })

    recorder.ondataavailable = (ev) => {
      if (ev.data.size > 0) {
        enqueueTranscribe(ev.data)
      }
    }
    recorder.onerror = () => setError('音频录制出错')
    recorder.onstop = () => {
      if (
        !intentionalStopRef.current
        && !fatalRef.current
        && enabledRef.current
        && streamRef.current
      ) {
        recordNextChunk()
      }
    }

    recorderRef.current = recorder
    recorder.start()
    clearChunkTimer()
    chunkTimerRef.current = window.setTimeout(() => {
      if (recorder.state === 'recording') {
        recorder.stop()
      }
    }, CHUNK_MS)
  }, [clearChunkTimer, enqueueTranscribe])

  const startRecording = useCallback(
    (stream: MediaStream) => {
      stopStream()
      streamRef.current = stream
      intentionalStopRef.current = false
      consecutiveErrorsRef.current = 0
      queueRef.current = []
      setListening(true)
      setCapturing(true)
      setError(null)
      recordNextChunk()
    },
    [recordNextChunk, stopStream]
  )

  const acquireStream = useCallback(async (): Promise<MediaStream> => {
    const video = getVideoElement()
    if (video && !video.paused && video.readyState >= 2) {
      const fromVideo = getCaptureStream(video)
      if (fromVideo) return fromVideo
    }

    const displayStream = await navigator.mediaDevices.getDisplayMedia({
      video: true,
      audio: true,
      preferCurrentTab: true,
    } as DisplayMediaStreamOptions)

    displayStream.getVideoTracks().forEach((t) => t.stop())

    const audioTracks = displayStream.getAudioTracks()
    if (audioTracks.length === 0) {
      displayStream.getTracks().forEach((t) => t.stop())
      throw new Error('未捕获到音频，请勾选「分享标签页音频」')
    }
    return new MediaStream(audioTracks)
  }, [getVideoElement])

  const start = useCallback(async () => {
    intentionalStopRef.current = false
    fatalRef.current = false
    consecutiveErrorsRef.current = 0
    setError(null)
    try {
      const stream = await acquireStream()
      startRecording(stream)
    } catch (e) {
      if ((e as Error).name === 'NotAllowedError') {
        setError('已取消屏幕共享，耳机模式需要授权捕获页面音频')
      } else {
        setError(e instanceof Error ? e.message : '无法捕获页面音频')
      }
      stopStream()
    }
  }, [acquireStream, startRecording, stopStream])

  const stop = useCallback(() => {
    intentionalStopRef.current = true
    clearChunkTimer()
    try {
      if (recorderRef.current?.state === 'recording') {
        recorderRef.current.stop()
      }
    } catch { /* ignore */ }
    stopStream()
  }, [clearChunkTimer, stopStream])

  const startRef = useRef(start)
  stopRef.current = stop
  startRef.current = start

  useEffect(() => {
    if (enabled) {
      startRef.current()
    } else {
      stopRef.current()
    }
    return () => stopRef.current()
  }, [enabled])

  return {
    supported: typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getDisplayMedia,
    listening,
    capturing,
    error,
    start,
    stop,
  }
}
