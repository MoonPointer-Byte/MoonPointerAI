import { isAndroidApp } from './platform'
import { pickBestVoice, resolveTtsLang, warmUpVoices } from '../utils/ttsVoice'
import '../types/androidBridge'

type DoneFn = () => void

const nativeCallbacks = new Map<string, DoneFn>()
let queueResetHandler: (() => void) | null = null
let webTtsPrimed = false

export function setTtsQueueResetHandler(handler: (() => void) | null): void {
  queueResetHandler = handler
}

export function initTtsEngine(): void {
  window.__moonTtsDone = (utteranceId: string) => {
    const cb = nativeCallbacks.get(utteranceId)
    if (cb) {
      nativeCallbacks.delete(utteranceId)
      cb()
    }
  }
  warmUpVoices()
}

export function isNativeTtsAvailable(): boolean {
  return isAndroidApp() && typeof window.MoonPointerAndroid?.speakTts === 'function'
}

function getWebSynth(): SpeechSynthesis | null {
  if (typeof window === 'undefined' || !window.speechSynthesis) return null
  return window.speechSynthesis
}

function isWebSynthBusy(): boolean {
  const synth = getWebSynth()
  return !!(synth && (synth.speaking || synth.pending))
}

function prepareWebSynth(): void {
  const synth = getWebSynth()
  if (!synth) return
  synth.getVoices()
  if (synth.paused) synth.resume()
}

/**
 * 必须在用户点击（勾选播报、🔊 等）时同步调用。
 * Chrome 要求首次 speak 发生在用户手势内，否则后续全部无声。
 */
export function unlockWebTts(): void {
  if (isNativeTtsAvailable()) return
  const synth = getWebSynth()
  if (!synth) return
  prepareWebSynth()
  if (webTtsPrimed) return

  const u = new SpeechSynthesisUtterance('。')
  u.lang = 'zh-CN'
  u.volume = 0.05
  u.rate = 3
  const mark = () => {
    webTtsPrimed = true
  }
  u.onend = mark
  u.onerror = mark
  synth.speak(u)
}

export function interruptPlayback(): void {
  nativeCallbacks.clear()
  window.MoonPointerAndroid?.stopTts?.()
  getWebSynth()?.cancel()
}

export function stopAllTts(): void {
  interruptPlayback()
  queueResetHandler?.()
}

function newUtteranceId(): string {
  return `u_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
}

function buildUtterance(
  text: string,
  lang: string,
  rate: number,
  useVoice: boolean
): SpeechSynthesisUtterance {
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = resolveTtsLang(lang)
  utterance.rate = rate
  utterance.pitch = 1
  utterance.volume = 1
  if (useVoice) {
    const voice = pickBestVoice(utterance.lang)
    if (voice) utterance.voice = voice
  }
  return utterance
}

function speakWebNow(
  trimmed: string,
  resolvedLang: string,
  safeRate: number,
  onEnd: DoneFn,
  onError?: DoneFn
): boolean {
  const synth = getWebSynth()
  if (!synth) return false

  prepareWebSynth()

  let finished = false
  const done = (fn: DoneFn) => {
    if (finished) return
    finished = true
    fn()
  }

  const start = (useVoice: boolean) => {
    const utterance = buildUtterance(trimmed, resolvedLang, safeRate, useVoice)

    utterance.onend = () => done(onEnd)
    utterance.onerror = (event) => {
      if (event.error === 'canceled') {
        done(onError ?? onEnd)
        return
      }
      if (useVoice) {
        start(false)
        return
      }
      done(onError ?? onEnd)
    }

    synth.speak(utterance)

    window.setTimeout(() => {
      if (synth.pending || synth.paused) synth.resume()
    }, 120)
  }

  start(true)
  return true
}

function speakWebDeferred(
  trimmed: string,
  resolvedLang: string,
  safeRate: number,
  onEnd: DoneFn,
  onError?: DoneFn
): boolean {
  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => {
      speakWebNow(trimmed, resolvedLang, safeRate, onEnd, onError)
    })
  })
  return true
}

export function speakTts(
  text: string,
  lang: string,
  rate: number,
  onEnd: DoneFn,
  onError?: () => void,
  options?: { defer?: boolean }
): boolean {
  const trimmed = text.trim()
  if (!trimmed) return false

  const resolvedLang = resolveTtsLang(lang)
  const safeRate = Math.min(2, Math.max(0.5, rate))
  const defer = options?.defer ?? false

  let finished = false
  const finish = (fn: DoneFn) => {
    if (finished) return
    finished = true
    fn()
  }

  const timeout = window.setTimeout(() => finish(onEnd), 45_000)
  const wrap = (fn: DoneFn) => () => {
    window.clearTimeout(timeout)
    finish(fn)
  }

  if (isNativeTtsAvailable()) {
    const id = newUtteranceId()
    nativeCallbacks.set(id, wrap(onEnd))

    const runNative = () => {
      try {
        window.MoonPointerAndroid!.speakTts!(trimmed, resolvedLang, safeRate, id)
      } catch {
        nativeCallbacks.delete(id)
        window.clearTimeout(timeout)
        finished = true
        onError?.()
      }
    }

    if (defer) window.setTimeout(runNative, 60)
    else runNative()
    return true
  }

  if (!getWebSynth()) {
    window.clearTimeout(timeout)
    onError?.()
    return false
  }

  const webEnd = wrap(onEnd)
  const webError = wrap(onError ?? onEnd)
  const started = defer
    ? speakWebDeferred(trimmed, resolvedLang, safeRate, webEnd, webError)
    : speakWebNow(trimmed, resolvedLang, safeRate, webEnd, webError)

  if (!started) {
    window.clearTimeout(timeout)
    onError?.()
  }
  return started
}

/** 手动点读：在用户点击回调里尽量同步 speak，避免手势失效 */
export function speakManual(text: string, lang: string, rate = 1.1): void {
  queueResetHandler?.()

  if (isNativeTtsAvailable()) {
    interruptPlayback()
    speakTts(text, lang, rate, () => {}, () => {}, { defer: true })
    return
  }

  const busy = isWebSynthBusy()
  if (busy) {
    interruptPlayback()
    speakTts(text, lang, rate, () => {}, () => {}, { defer: true })
  } else {
    nativeCallbacks.clear()
    speakTts(text, lang, rate, () => {}, () => {}, { defer: false })
  }
}
