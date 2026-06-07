import { useCallback, useEffect, useRef, useState } from 'react'
import type { SubtitleSegment } from '../types'
import { getClientSessionId } from '../utils/clientSession'

export type NoteMode = 'manual' | 'ai'
export type AiStatus = 'IDLE' | 'GENERATING' | 'DONE' | 'FAILED'

export interface NoteRecord {
  id?: number
  title: string
  videoUrl: string
  sourceLang: string
  targetLang: string
  manualContent: string
  aiContent: string
  preferredMode: NoteMode
  aiStatus: AiStatus
  lastAiGeneratedAt?: string
}

function mapMode(raw?: string): NoteMode {
  return raw?.toUpperCase() === 'AI' ? 'ai' : 'manual'
}

function mapAiStatus(raw?: string): AiStatus {
  const s = (raw ?? 'IDLE').toUpperCase()
  if (s === 'GENERATING' || s === 'DONE' || s === 'FAILED') return s
  return 'IDLE'
}

export function useNotes(
  serverUrl: string,
  sourceLang: string,
  targetLang: string,
  segments: SubtitleSegment[] = [],
  active = false
) {
  const [note, setNote] = useState<NoteRecord>({
    title: '',
    videoUrl: '',
    sourceLang,
    targetLang,
    manualContent: '',
    aiContent: '',
    preferredMode: 'manual',
    aiStatus: 'IDLE',
  })
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(true)
  const [dbReady, setDbReady] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [segmentCount, setSegmentCount] = useState(0)

  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const syncTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastSyncedCountRef = useRef(0)
  const clientSessionId = getClientSessionId()
  const apiBase = serverUrl.replace(/\/$/, '')

  useEffect(() => {
    fetch(`${apiBase}/api/notes/current?clientSessionId=${clientSessionId}`)
      .then((r) => {
        if (!r.ok) throw new Error('notes unavailable')
        return r.json()
      })
      .then((data) => {
        if (data) {
          setNote({
            id: data.id,
            title: data.title ?? '',
            videoUrl: data.videoUrl ?? '',
            manualContent: data.manualContent ?? data.content ?? '',
            aiContent: data.aiContent ?? '',
            preferredMode: mapMode(data.preferredMode),
            aiStatus: mapAiStatus(data.aiStatus),
            sourceLang: data.sourceLang ?? sourceLang,
            targetLang: data.targetLang ?? targetLang,
            lastAiGeneratedAt: data.lastAiGeneratedAt,
          })
        }
        setDbReady(true)
      })
      .catch(() => setDbReady(false))
  }, [apiBase, clientSessionId, sourceLang, targetLang])

  const persist = useCallback(
    (next: NoteRecord) => {
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current)
      setSaved(false)
      saveTimerRef.current = setTimeout(async () => {
        setSaving(true)
        try {
          const res = await fetch(`${apiBase}/api/notes`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              clientSessionId,
              title: next.title,
              videoUrl: next.videoUrl,
              manualContent: next.manualContent,
              content: next.manualContent,
              aiContent: next.aiContent,
              preferredMode: next.preferredMode.toUpperCase(),
              sourceLang: next.sourceLang,
              targetLang: next.targetLang,
            }),
          })
          if (res.ok) {
            const data = await res.json()
            setNote((prev) => ({
              ...prev,
              id: data.id,
              aiStatus: mapAiStatus(data.aiStatus),
              lastAiGeneratedAt: data.lastAiGeneratedAt,
            }))
            setSaved(true)
          }
        } catch {
          setSaved(false)
        } finally {
          setSaving(false)
        }
      }, 800)
    },
    [apiBase, clientSessionId]
  )

  const updateNote = useCallback(
    (patch: Partial<NoteRecord>) => {
      setNote((prev) => {
        const next = {
          ...prev,
          ...patch,
          sourceLang: patch.sourceLang ?? prev.sourceLang,
          targetLang: patch.targetLang ?? prev.targetLang,
        }
        persist(next)
        return next
      })
    },
    [persist]
  )

  const setMode = useCallback(
    (mode: NoteMode) => updateNote({ preferredMode: mode }),
    [updateNote]
  )

  const syncSegments = useCallback(async () => {
    const finalized = segments.filter((s) => s.isFinal && s.sourceText.trim())
    if (finalized.length === 0) return
    if (finalized.length === lastSyncedCountRef.current) return

    try {
      const res = await fetch(`${apiBase}/api/notes/segments/sync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          clientSessionId,
          segments: finalized.map((s) => ({
            segmentId: s.id,
            sourceText: s.sourceText,
            translatedText: s.translatedText,
            timestampMs: s.timestampMs ?? 0,
            isFinal: true,
          })),
        }),
      })
      if (res.ok) {
        lastSyncedCountRef.current = finalized.length
        setSegmentCount(finalized.length)
        setDbReady(true)
      }
    } catch {
      setDbReady(false)
    }
  }, [apiBase, clientSessionId, segments])

  const generateAiNotes = useCallback(async () => {
    setGenerating(true)
    setNote((prev) => ({ ...prev, aiStatus: 'GENERATING' }))
    try {
      await syncSegments()
      const res = await fetch(`${apiBase}/api/notes/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          clientSessionId,
          sourceLang,
          targetLang,
        }),
        signal: AbortSignal.timeout(120_000),
      })
      let data: { message?: string; aiContent?: string; noteId?: number; aiStatus?: string; lastAiGeneratedAt?: string } = {}
      try {
        data = await res.json()
      } catch {
        /* ignore */
      }
      if (!res.ok) {
        throw new Error(data.message || `AI 笔记生成失败 (${res.status})，请重启后端后重试`)
      }
      setNote((prev) => ({
        ...prev,
        id: data.noteId ?? prev.id,
        aiContent: data.aiContent ?? '',
        aiStatus: mapAiStatus(data.aiStatus),
        lastAiGeneratedAt: data.lastAiGeneratedAt,
        preferredMode: 'ai',
      }))
      setSaved(true)
    } catch (e) {
      setNote((prev) => ({ ...prev, aiStatus: 'FAILED' }))
      throw e
    } finally {
      setGenerating(false)
    }
  }, [apiBase, clientSessionId, sourceLang, targetLang, syncSegments])

  useEffect(() => {
    setNote((prev) => ({ ...prev, sourceLang, targetLang }))
  }, [sourceLang, targetLang])

  const segmentsRef = useRef(segments)
  segmentsRef.current = segments

  const finalizedCount = segments.filter((s) => s.isFinal).length

  useEffect(() => {
    setSegmentCount(finalizedCount)
  }, [finalizedCount])

  useEffect(() => {
    if (!active || finalizedCount === 0) return
    if (syncTimerRef.current) clearTimeout(syncTimerRef.current)
    syncTimerRef.current = setTimeout(() => {
      const finalized = segmentsRef.current.filter((s) => s.isFinal && s.sourceText.trim())
      if (finalized.length === 0 || finalized.length === lastSyncedCountRef.current) return
      fetch(`${apiBase}/api/notes/segments/sync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          clientSessionId,
          segments: finalized.map((s) => ({
            segmentId: s.id,
            sourceText: s.sourceText,
            translatedText: s.translatedText,
            timestampMs: s.timestampMs ?? 0,
            isFinal: true,
          })),
        }),
      })
        .then((r) => {
          if (r.ok) {
            lastSyncedCountRef.current = finalized.length
            setDbReady(true)
          }
        })
        .catch(() => setDbReady(false))
    }, 4000)
    return () => {
      if (syncTimerRef.current) clearTimeout(syncTimerRef.current)
    }
  }, [active, finalizedCount, apiBase, clientSessionId])

  return {
    note,
    updateNote,
    setMode,
    generateAiNotes,
    syncSegments,
    saving,
    saved,
    dbReady,
    generating,
    segmentCount,
  }
}
