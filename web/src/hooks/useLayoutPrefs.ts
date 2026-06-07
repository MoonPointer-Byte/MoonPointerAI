import { useCallback, useEffect, useRef, useState } from 'react'

export interface LayoutPrefs {
  sideWidthPercent: number
  videoSplitPercent: number
  panelRadius: number
  borderWidth: number
  panelGap: number
  workspacePadding: number
}

export const DEFAULT_LAYOUT: LayoutPrefs = {
  sideWidthPercent: 40,
  videoSplitPercent: 52,
  panelRadius: 16,
  borderWidth: 1,
  panelGap: 10,
  workspacePadding: 14,
}

const STORAGE_KEY = 'translator-layout'

function clamp(n: number, min: number, max: number) {
  return Math.min(max, Math.max(min, n))
}

function loadLayout(): LayoutPrefs {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return { ...DEFAULT_LAYOUT, ...JSON.parse(raw) }
  } catch { /* ignore */ }
  return DEFAULT_LAYOUT
}

export function applyLayoutVars(root: HTMLElement | null, layout: LayoutPrefs) {
  if (!root) return
  root.style.setProperty('--panel-radius', `${layout.panelRadius}px`)
  root.style.setProperty('--panel-border', `${layout.borderWidth}px`)
  root.style.setProperty('--panel-gap', `${layout.panelGap}px`)
  root.style.setProperty('--workspace-pad', `${layout.workspacePadding}px`)
  root.style.setProperty('--side-width', `${layout.sideWidthPercent}%`)
  root.style.setProperty('--video-split', `${layout.videoSplitPercent}%`)
}

function beginDrag(cursor: string, onMove: (ev: MouseEvent) => void, onEnd: () => void) {
  document.body.classList.add('is-resizing')
  document.body.style.cursor = cursor

  let raf = 0
  const move = (ev: MouseEvent) => {
    ev.preventDefault()
    if (raf) return
    raf = requestAnimationFrame(() => {
      raf = 0
      onMove(ev)
    })
  }
  const up = () => {
    if (raf) cancelAnimationFrame(raf)
    document.body.classList.remove('is-resizing')
    document.body.style.cursor = ''
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    onEnd()
  }
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

/** 拖拽时只改 CSS 变量，松手后再写入 state（避免整页重渲染） */
export function createColumnSplitter(
  getContainer: () => HTMLElement | null,
  getStyleRoot: () => HTMLElement | null,
  onCommit: (sideWidthPercent: number) => void,
  min = 24,
  max = 60
) {
  return (e: React.MouseEvent) => {
    e.preventDefault()
    let last = 0
    const update = (clientX: number) => {
      const el = getContainer()
      const root = getStyleRoot()
      if (!el || !root) return
      const rect = el.getBoundingClientRect()
      last = clamp(((rect.right - clientX) / rect.width) * 100, min, max)
      root.style.setProperty('--side-width', `${last}%`)
    }
    update(e.clientX)
    beginDrag('col-resize', (ev) => update(ev.clientX), () => onCommit(last))
  }
}

export function createRowSplitter(
  getContainer: () => HTMLElement | null,
  getStyleRoot: () => HTMLElement | null,
  onCommit: (topHeightPercent: number) => void,
  min = 25,
  max = 75
) {
  return (e: React.MouseEvent) => {
    e.preventDefault()
    let last = 0
    const update = (clientY: number) => {
      const el = getContainer()
      const root = getStyleRoot()
      if (!el || !root) return
      const rect = el.getBoundingClientRect()
      last = clamp(((clientY - rect.top) / rect.height) * 100, min, max)
      root.style.setProperty('--video-split', `${last}%`)
    }
    update(e.clientY)
    beginDrag('row-resize', (ev) => update(ev.clientY), () => onCommit(last))
  }
}

export function useLayoutPrefs(appRef: React.RefObject<HTMLElement | null>) {
  const [layout, setLayout] = useState<LayoutPrefs>(loadLayout)
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    applyLayoutVars(appRef.current, layout)
  }, [layout, appRef])

  const persist = useCallback((next: LayoutPrefs) => {
    if (saveTimer.current) clearTimeout(saveTimer.current)
    saveTimer.current = setTimeout(() => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    }, 400)
  }, [])

  const updateLayout = useCallback(
    (patch: Partial<LayoutPrefs>) => {
      setLayout((prev) => {
        const next = { ...prev, ...patch }
        persist(next)
        return next
      })
    },
    [persist]
  )

  const resetLayout = useCallback(() => {
    setLayout(DEFAULT_LAYOUT)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_LAYOUT))
    applyLayoutVars(appRef.current, DEFAULT_LAYOUT)
  }, [appRef])

  return { layout, updateLayout, resetLayout }
}
