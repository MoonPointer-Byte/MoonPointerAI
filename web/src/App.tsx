import { useCallback, useEffect, useRef, useState } from 'react'
import { EmptyState } from './components/EmptyState'
import { LayoutSettings } from './components/LayoutSettings'
import { MobileTabBar, type MobileTab } from './components/MobileTabBar'
import { MoonPointerLogo } from './components/MoonPointerLogo'
import { ThemeToggle } from './components/ThemeToggle'
import { TtsPaceToggle } from './components/TtsPaceToggle'
import { NotesPanel } from './components/NotesPanel'
import { ResizeHandle } from './components/ResizeHandle'
import { sanitizeTranslation, TranscriptBlock } from './components/TranscriptBlock'
import { VideoPanel } from './components/VideoPanel'
import { createColumnSplitter, createRowSplitter, useLayoutPrefs } from './hooks/useLayoutPrefs'
import { labelFor, speechTagFor } from './constants/languages'
import { useLanguages } from './hooks/useLanguages'
import { useMobileLayout } from './hooks/useMobileLayout'
import { useTheme } from './hooks/useTheme'
import { useNotes } from './hooks/useNotes'
import { useChunkSentenceSegmenter } from './hooks/useChunkSentenceSegmenter'
import { useSentenceSegmenter } from './hooks/useSentenceSegmenter'
import { useSimultaneousSpeech } from './hooks/useSimultaneousSpeech'
import { useSimultaneousTts } from './hooks/useSimultaneousTts'
import { useAppHealth } from './hooks/useAppHealth'
import { useSpeechRecognition } from './hooks/useSpeechRecognition'
import { useTabAudioSpeech } from './hooks/useTabAudioSpeech'
import { useWebSocket } from './hooks/useWebSocket'
import type { AppConfig, SubtitleSegment, WsMessage } from './types'
import { lookupInstant, translatePhraseLocally } from './lib/instantDict'
import { formatElapsed } from './utils/formatTime'
import { isAndroidApp } from './utils/platform'
import { speakManual, unlockWebTts } from './utils/ttsEngine'
import './App.css'

const androidShell = isAndroidApp()

const DEFAULT_CONFIG: AppConfig = {
  serverUrl: import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin,
  sourceLang: 'en',
  targetLang: 'zh',
  audioInput: 'mic',
  ttsEnabled: false,
  ttsMatchSource: false,
  showSource: true,
  autoScroll: true,
  showTranslation: true,
}

function loadConfig(): AppConfig {
  const bridge = (window as Window & { MoonPointerAndroid?: { getServerUrl(): string } })
    .MoonPointerAndroid
  if (bridge?.getServerUrl) {
    try {
      const saved = localStorage.getItem('translator-config')
      const parsed = saved ? JSON.parse(saved) : {}
      const audioInput = parsed.audioInput === 'headphone' ? 'headphone' : 'mic'
      const ttsMatchSource = parsed.ttsMatchSource === true
      return {
        ...DEFAULT_CONFIG,
        ...parsed,
        serverUrl: bridge.getServerUrl(),
        audioInput,
        ttsMatchSource,
      }
    } catch { /* fall through */ }
    return { ...DEFAULT_CONFIG, serverUrl: bridge.getServerUrl() }
  }

  try {
    const saved = localStorage.getItem('translator-config')
    if (saved) {
      const parsed = JSON.parse(saved)
      const { showNotes: _, ...rest } = parsed
      const audioInput = rest.audioInput === 'headphone' ? 'headphone' : 'mic'
      const ttsMatchSource = rest.ttsMatchSource === true
      return { ...DEFAULT_CONFIG, ...rest, audioInput, ttsMatchSource }
    }
  } catch { /* ignore */ }
  return DEFAULT_CONFIG
}

export default function App() {
  const [config, setConfig] = useState<AppConfig>(loadConfig)
  const [segments, setSegments] = useState<SubtitleSegment[]>([])
  const [active, setActive] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [liveSource, setLiveSource] = useState('')
  const [liveTranslation, setLiveTranslation] = useState('')
  const [elapsedMs, setElapsedMs] = useState(0)
  const [apiError, setApiError] = useState<string | null>(null)
  const [mobileTab, setMobileTab] = useState<MobileTab>('transcript')
  const isMobileLayout = useMobileLayout()
  const { theme, toggleTheme } = useTheme()

  const appRef = useRef<HTMLDivElement>(null)
  const workspaceRef = useRef<HTMLDivElement>(null)
  const sideRef = useRef<HTMLDivElement>(null)
  const videoElRef = useRef<HTMLVideoElement>(null)
  const getVideoElement = useCallback(() => videoElRef.current, [])
  const { layout, updateLayout, resetLayout } = useLayoutPrefs(appRef)

  const languages = useLanguages(config.serverUrl)
  const { sttConfigured } = useAppHealth(config.serverUrl)
  const useHeadphone = config.audioInput === 'headphone'
  const {
    note,
    updateNote,
    setMode,
    generateAiNotes,
    saving,
    saved,
    dbReady,
    generating,
    segmentCount,
  } = useNotes(config.serverUrl, config.sourceLang, config.targetLang, segments, active)

  const transcriptEndRef = useRef<HTMLDivElement>(null)
  const sessionStartRef = useRef(0)
  const liveSegmentIdRef = useRef<string | null>(null)
  const liveTranslationRef = useRef('')
  const ttsEnabledRef = useRef(config.ttsEnabled)
  const onStreamTtsRef = useRef<(segId: string, text: string, isFinal: boolean) => void>(() => {})
  const reportSourceRef = useRef<(text: string, isFinal: boolean, segmentId: string) => void>(() => {})
  const speakCorrectionRef = useRef<(text: string, segmentId?: string) => void>(() => {})

  ttsEnabledRef.current = config.ttsEnabled
  liveTranslationRef.current = liveTranslation

  const nowOffset = () =>
    sessionStartRef.current ? Date.now() - sessionStartRef.current : 0

  const handleWsMessage = useCallback((msg: WsMessage) => {
    switch (msg.type) {
      case 'TRANSLATION': {
        const segmentId = msg.segmentId
        if (!segmentId) break
        const isFinal = !!msg.isFinal

        if (!isFinal) {
          const preview = sanitizeTranslation(msg.translatedText || '')
          if (segmentId === liveSegmentIdRef.current) {
            setLiveTranslation(preview)
          } else {
            setSegments((prev) =>
              prev.map((s) =>
                s.id === segmentId && !s.isFinal
                  ? { ...s, translatedText: preview || s.translatedText }
                  : s
              )
            )
          }
          break
        }

        const translatedText = sanitizeTranslation(msg.translatedText || '')
        const canFinalize = translatedText.length > 0

        setSegments((prev) => {
          const idx = prev.findIndex((s) => s.id === segmentId)
          if (idx >= 0) {
            const next = [...prev]
            next[idx] = {
              ...next[idx],
              sourceText: msg.text || next[idx].sourceText,
              translatedText: translatedText || next[idx].translatedText,
              isFinal: canFinalize,
            }
            return next
          }
          if (!canFinalize) return prev
          return [
            ...prev,
            {
              id: segmentId,
              sourceText: msg.text || '',
              translatedText,
              isFinal: true,
              corrected: false,
              timestampMs: nowOffset(),
            },
          ]
        })
        if (ttsEnabledRef.current && canFinalize) {
          onStreamTtsRef.current(segmentId, translatedText, true)
        }
        break
      }
      case 'CORRECTION':
        if (!msg.segmentId) break
        setSegments((prev) =>
          prev.map((s) =>
            s.id === msg.segmentId
              ? { ...s, translatedText: sanitizeTranslation(msg.translatedText || '') || s.translatedText, corrected: true }
              : s
          )
        )
        if (msg.translatedText && ttsEnabledRef.current && msg.segmentId) {
          speakCorrectionRef.current(
            sanitizeTranslation(msg.translatedText),
            msg.segmentId
          )
        }
        break
      case 'ERROR':
        if (msg.message) setApiError(msg.message)
        break
    }
  }, [])

  const { connected, llmConfigured, connect, disconnect, send } = useWebSocket(
    config.serverUrl,
    handleWsMessage
  )

  const ttsLang = speechTagFor(config.targetLang, languages)
  const { onStreamUpdate, reportSourceActivity, speakCorrection, stop: stopTts } =
    useSimultaneousTts(config.ttsEnabled, ttsLang, config.ttsMatchSource)

  const speakLine = useCallback((text: string, lang: string) => {
    unlockWebTts()
    speakManual(text, lang)
  }, [])
  onStreamTtsRef.current = onStreamUpdate
  reportSourceRef.current = reportSourceActivity
  speakCorrectionRef.current = speakCorrection

  const { sendSpeech, reset: resetSpeech } = useSimultaneousSpeech(
    send,
    config.sourceLang,
    config.targetLang
  )

  const { processChunk, flush, reset: resetSegmenter } = useSentenceSegmenter()
  const {
    processChunk: processSttChunk,
    flush: flushSttChunk,
    reset: resetSttSegmenter,
  } = useChunkSentenceSegmenter()

  const instantPreview = useCallback(
    (text: string) => {
      return (
        translatePhraseLocally(text, config.sourceLang, config.targetLang)
        ?? lookupInstant(text, config.sourceLang, config.targetLang)
      )
    },
    [config.sourceLang, config.targetLang]
  )

  const applySentenceEvent = useCallback(
    (text: string, segmentId: string, isComplete: boolean) => {
      if (isComplete) {
        const preview = liveTranslationRef.current
        setLiveSource('')
        setLiveTranslation('')
        liveSegmentIdRef.current = null
        setSegments((prev) => [
          ...prev,
          {
            id: segmentId,
            sourceText: text,
            translatedText: preview,
            isFinal: false,
            corrected: false,
            timestampMs: nowOffset(),
          },
        ])
        reportSourceRef.current(text, true, segmentId)
        sendSpeech(text, true, segmentId)
        return
      }

      setLiveSource(text)
      liveSegmentIdRef.current = segmentId
      const local = instantPreview(text)
      if (local) setLiveTranslation(local)
      reportSourceRef.current(text, false, segmentId)
      sendSpeech(text, false, segmentId)
    },
    [instantPreview, sendSpeech]
  )

  const onSpeechResult = useCallback(
    (text: string, isFinal: boolean) => {
      if (!text) return
      const events = useHeadphone
        ? processSttChunk(text)
        : processChunk(text, isFinal)
      for (const event of events) {
        applySentenceEvent(event.text, event.segmentId, event.isComplete)
      }
    },
    [useHeadphone, processChunk, processSttChunk, applySentenceEvent]
  )

  const speechLang = speechTagFor(config.sourceLang, languages)
  const micEnabled = active && connected && !useHeadphone
  const headphoneEnabled = active && connected && useHeadphone

  const { supported: micSupported, listening: micListening, error: micError } = useSpeechRecognition(
    speechLang,
    onSpeechResult,
    micEnabled
  )

  const {
    supported: headphoneSupported,
    listening: headphoneListening,
    error: headphoneError,
  } = useTabAudioSpeech(
    config.serverUrl,
    config.sourceLang,
    onSpeechResult,
    headphoneEnabled,
    getVideoElement
  )

  const speechSupported = useHeadphone ? headphoneSupported : micSupported
  const listening = useHeadphone ? headphoneListening : micListening
  const speechError = useHeadphone ? headphoneError : micError

  const langConflict = config.sourceLang === config.targetLang

  useEffect(() => {
    if (!active) return
    const id = setInterval(() => {
      if (sessionStartRef.current) setElapsedMs(Date.now() - sessionStartRef.current)
    }, 1000)
    return () => clearInterval(id)
  }, [active])

  const segmentsLength = segments.length
  useEffect(() => {
    if (!config.autoScroll) return
    transcriptEndRef.current?.scrollIntoView({ behavior: 'auto', block: 'end' })
  }, [segmentsLength, liveSource, config.autoScroll])

  const handleStart = () => {
    if (langConflict) return
    if (useHeadphone && !sttConfigured) {
      setApiError('耳机模式需配置语音识别 API Key，见 application-local.yml 中 translator.stt（推荐硅基流动）')
      return
    }
    connect()
    setApiError(null)
    setSegments([])
    setLiveSource('')
    setLiveTranslation('')
    liveSegmentIdRef.current = null
    setElapsedMs(0)
    sessionStartRef.current = Date.now()
    resetSpeech()
    resetSegmenter()
    resetSttSegmenter()
    if (config.ttsEnabled) unlockWebTts()
    setActive(true)
    setTimeout(() => {
      send({
        type: 'CONFIG',
        sourceLang: config.sourceLang,
        targetLang: config.targetLang,
        ttsEnabled: config.ttsEnabled,
      })
    }, 400)
  }

  const handleStop = () => {
    const pending = useHeadphone ? flushSttChunk() : flush()
    if (pending) {
      applySentenceEvent(pending.text, pending.segmentId, true)
    }
    setActive(false)
    stopTts()
    resetSpeech()
    resetSegmenter()
    resetSttSegmenter()
    disconnect()
    setLiveSource('')
    setLiveTranslation('')
    liveSegmentIdRef.current = null
    sessionStartRef.current = 0
  }

  const updateConfig = (patch: Partial<AppConfig>) => {
    const next = { ...config, ...patch }
    setConfig(next)
    localStorage.setItem('translator-config', JSON.stringify(next))
  }

  const finalizedSegments = segments.filter((s) => s.isFinal)
  const pendingSegments = segments.filter((s) => !s.isFinal)
  const isIdle = !active && finalizedSegments.length === 0 && !liveSource
  const sourceLabel = labelFor(config.sourceLang, languages)
  const targetLabel = labelFor(config.targetLang, languages)

  const onSideResize = createColumnSplitter(
    () => workspaceRef.current,
    () => appRef.current,
    (pct) => updateLayout({ sideWidthPercent: pct })
  )

  const onVideoResize = createRowSplitter(
    () => sideRef.current,
    () => appRef.current,
    (pct) => updateLayout({ videoSplitPercent: pct })
  )

  const appClassName = [
    'app',
    androidShell && 'app--android',
    isMobileLayout && 'app--mobile',
    isIdle && 'app--idle',
  ].filter(Boolean).join(' ')

  const workspaceClassName = [
    'workspace',
    isMobileLayout && 'workspace--mobile',
  ].filter(Boolean).join(' ')

  const paneHidden = (tab: MobileTab) =>
    isMobileLayout && mobileTab !== tab ? 'mobile-pane-hidden' : ''

  return (
    <div className={appClassName} ref={appRef}>
      <header className="toolbar">
        <div className="toolbar-brand">
          <MoonPointerLogo size={42} className="toolbar-logo" />
          <div className="toolbar-brand-text">
            <span className="toolbar-title">MoonPointer</span>
            <span className="toolbar-sub">实时字幕 · 同声传译 · 学习笔记</span>
          </div>
        </div>

        <div className="lang-switcher">
          <select
            className="lang-select"
            value={config.sourceLang}
            onChange={(e) => updateConfig({ sourceLang: e.target.value })}
            disabled={active}
          >
            {languages.map((l) => (
              <option key={`s-${l.code}`} value={l.code}>{l.label}</option>
            ))}
          </select>
          <span className="lang-arrow">→</span>
          <select
            className="lang-select"
            value={config.targetLang}
            onChange={(e) => updateConfig({ targetLang: e.target.value })}
            disabled={active}
          >
            {languages.map((l) => (
              <option key={`t-${l.code}`} value={l.code}>{l.label}</option>
            ))}
          </select>
        </div>

        <div className="audio-input-switcher">
          <button
            type="button"
            className={`audio-input-btn ${config.audioInput === 'mic' ? 'active' : ''}`}
            onClick={() => updateConfig({ audioInput: 'mic' })}
            disabled={active}
            title="通过麦克风拾音，适合外放或面对面"
          >
            麦克风
          </button>
          <button
            type="button"
            className={`audio-input-btn ${config.audioInput === 'headphone' ? 'active' : ''}`}
            onClick={() => updateConfig({ audioInput: 'headphone' })}
            disabled={active}
            title="捕获页面/标签页音频，适合戴耳机看视频"
          >
            耳机模式
          </button>
        </div>

        <div className="toolbar-toggles">
          <label className="toggle-chip">
            <input
              type="checkbox"
              checked={config.autoScroll}
              onChange={(e) => updateConfig({ autoScroll: e.target.checked })}
            />
            <span>自动滚动</span>
          </label>
          <label className="toggle-chip">
            <input
              type="checkbox"
              checked={config.showTranslation}
              onChange={(e) => updateConfig({ showTranslation: e.target.checked })}
            />
            <span>显示译文</span>
          </label>
          <label className="toggle-chip">
            <input
              type="checkbox"
              checked={config.ttsEnabled}
              onChange={(e) => {
                updateConfig({ ttsEnabled: e.target.checked })
                if (e.target.checked) unlockWebTts()
              }}
            />
            <span>语音播报</span>
          </label>
          <TtsPaceToggle
            ttsEnabled={config.ttsEnabled}
            matchSource={config.ttsMatchSource}
            onChange={(next) => updateConfig(next)}
          />
        </div>

        <div className="toolbar-right">
          {active && (
            <span className={`live-badge ${connected && listening ? 'on' : ''}`}>
              {formatElapsed(elapsedMs)}
            </span>
          )}
          <ThemeToggle theme={theme} onToggle={toggleTheme} />
          {!androidShell && (
            <button className="icon-btn" onClick={() => setShowSettings(!showSettings)} aria-label="设置">
              ⚙
            </button>
          )}
        </div>
      </header>

      {showSettings && (
        <div className="settings-panel">
          <div className="settings-bar">
            <label>
              服务器地址
              <input value={config.serverUrl} onChange={(e) => updateConfig({ serverUrl: e.target.value })} />
            </label>
            <span className="info">严格翻译模式：仅输出译文，无解释</span>
            {!llmConfigured && connected && <span className="warn">DeepSeek API 未配置</span>}
            {!dbReady && <span className="warn">MySQL 未连接，笔记无法保存</span>}
            {!speechSupported && <span className="warn">请用 Chrome / Edge 打开</span>}
            {useHeadphone && !sttConfigured && (
              <span className="warn">耳机模式：请在 application-local.yml 配置硅基流动 STT Key</span>
            )}
          </div>
          <LayoutSettings layout={layout} onChange={updateLayout} onReset={resetLayout} />
        </div>
      )}

      {(speechError || apiError || langConflict) && (
        <div className="alert-bar">
          {langConflict ? '源语言与目标语言不能相同' : speechError || apiError}
        </div>
      )}

      <div className={workspaceClassName} ref={workspaceRef} data-mobile-tab={mobileTab}>
        <main className={`transcript-column glass-panel ${paneHidden('transcript')}`}>
          <div className="column-header">
            <h2>实时字幕</h2>
            <span className="column-meta">{sourceLabel} → {targetLabel}</span>
          </div>

          <div className="transcript-panel">
            {isIdle ? (
              <EmptyState
                targetLabel={targetLabel}
                onStart={handleStart}
                disabled={!speechSupported || langConflict}
              />
            ) : (
              <div className="transcript-stream">
                {finalizedSegments.map((seg) => (
                  <TranscriptBlock
                    key={seg.id}
                    source={seg.sourceText}
                    translation={seg.translatedText}
                    time={seg.timestampMs}
                    showSource={config.showSource}
                    showTranslation={config.showTranslation}
                    corrected={seg.corrected}
                    onSpeakSource={(t) => speakLine(t, speechLang)}
                    onSpeakTranslation={(t) => speakLine(t, ttsLang)}
                  />
                ))}
                {pendingSegments.map((seg) => (
                  <TranscriptBlock
                    key={seg.id}
                    source={seg.sourceText}
                    translation={
                      seg.translatedText ||
                      (config.showTranslation ? (apiError ? '翻译失败' : '翻译中…') : '')
                    }
                    time={seg.timestampMs}
                    showSource={config.showSource}
                    showTranslation={config.showTranslation}
                    pending
                    onSpeakSource={(t) => speakLine(t, speechLang)}
                    onSpeakTranslation={(t) => speakLine(t, ttsLang)}
                  />
                ))}
                {liveSource && (
                  <TranscriptBlock
                    source={liveSource}
                    translation={liveTranslation || (config.showTranslation ? '…' : '')}
                    time={nowOffset()}
                    showSource={config.showSource}
                    showTranslation={config.showTranslation}
                    live
                  />
                )}
                <div ref={transcriptEndRef} />
              </div>
            )}
          </div>

          {!isIdle && (
            <footer className="transcript-footer">
              {active ? (
                <>
                  <span className="bottom-status">
                    <span className={`dot ${connected && listening ? 'on' : ''}`} />
                    {connected && listening
                      ? useHeadphone
                        ? '页面音频转写中'
                        : '实时转写中'
                      : '连接中…'}
                  </span>
                  <button type="button" className="stop-btn" onClick={handleStop}>停止传译</button>
                </>
              ) : (
                <button
                  type="button"
                  className="start-btn start-btn-inline"
                  onClick={handleStart}
                  disabled={!speechSupported || langConflict}
                >
                  开始传译
                </button>
              )}
            </footer>
          )}
        </main>

        <ResizeHandle
          axis="column"
          onMouseDown={onSideResize}
          label="拖动调节字幕区与侧栏宽度"
        />

        <aside className={`side-column ${isMobileLayout ? 'side-column--mobile' : ''}`} ref={sideRef}>
          <div className={`side-slot side-slot--video ${paneHidden('video')}`}>
            <VideoPanel
              serverUrl={config.serverUrl}
              videoUrl={note.videoUrl}
              onVideoUrlChange={(url) => updateNote({ videoUrl: url })}
              videoRef={videoElRef}
            />
          </div>

          <ResizeHandle
            axis="row"
            onMouseDown={onVideoResize}
            label="拖动调节视频与笔记高度"
          />

          <div className={`side-slot side-slot--notes ${paneHidden('notes')}`}>
            <NotesPanel
              note={note}
              mode={note.preferredMode}
              onModeChange={setMode}
              onChange={updateNote}
              onGenerate={generateAiNotes}
              saving={saving}
              saved={saved}
              dbReady={dbReady}
              generating={generating}
              segmentCount={segmentCount}
            />
          </div>
        </aside>
      </div>

      {isMobileLayout && (
        <MobileTabBar active={mobileTab} onChange={setMobileTab} />
      )}
    </div>
  )
}
