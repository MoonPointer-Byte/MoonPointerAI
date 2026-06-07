import { useCallback, useEffect, useState, type RefObject } from 'react'
import { extractUrlFromText } from '../utils/extractUrl'
import { parseVideoUrl, type ParsedVideo } from '../utils/videoUrl'

interface VideoPanelProps {
  serverUrl: string
  videoUrl: string
  onVideoUrlChange: (url: string) => void
  videoRef?: RefObject<HTMLVideoElement>
}

function needsBackendResolve(trimmed: string, local: ParsedVideo): boolean {
  if (!trimmed) return false
  if (local.embedUrl || local.src) return false
  const lower = trimmed.toLowerCase()
  if (lower.includes('v.douyin.com') || lower.includes('douyin.com')) return true
  if (lower.includes('b23.tv') || lower.includes('bilibili.com')) return true
  return local.type === 'unknown'
}

function toParsedVideo(data: Record<string, string>, fallbackUrl: string): ParsedVideo {
  const rawType = data.type || 'unknown'
  const type: ParsedVideo['type'] =
    rawType === 'link' ? 'douyin' : (rawType as ParsedVideo['type'])
  return {
    type,
    embedUrl: data.embedUrl,
    src: data.src,
    originalUrl: data.originalUrl || fallbackUrl,
    videoId: data.videoId,
  }
}

export function VideoPanel({ serverUrl, videoUrl, onVideoUrlChange, videoRef }: VideoPanelProps) {
  const [input, setInput] = useState(videoUrl)
  const [parsed, setParsed] = useState<ParsedVideo | null>(null)
  const [loading, setLoading] = useState(false)
  const [resolveHint, setResolveHint] = useState('')
  const [embedFailed, setEmbedFailed] = useState(false)

  const resolve = useCallback(
    async (raw: string) => {
      const trimmed = extractUrlFromText(raw)
      if (!trimmed) {
        setParsed(null)
        setResolveHint('')
        return
      }

      let result = parseVideoUrl(trimmed)
      setResolveHint('')

      if (needsBackendResolve(trimmed, result)) {
        setLoading(true)
        try {
          const base = serverUrl.replace(/\/$/, '')
          const res = await fetch(
            `${base}/api/video/resolve?url=${encodeURIComponent(raw.trim())}`
          )
          if (res.ok) {
            const data = await res.json()
            if (data.embedUrl || data.src) {
              result = toParsedVideo(data, trimmed)
            } else if (data.originalUrl) {
              result = {
                ...result,
                originalUrl: data.originalUrl,
                type: result.type === 'unknown' ? 'douyin' : result.type,
              }
            }
            if (data.message) setResolveHint(data.message)
          } else {
            setResolveHint('链接解析失败，可尝试粘贴完整视频地址')
          }
        } catch {
          setResolveHint('无法连接后端解析服务')
        } finally {
          setLoading(false)
        }
      }

      setParsed(result)
      setEmbedFailed(false)
    },
    [serverUrl]
  )

  useEffect(() => {
    setInput(videoUrl)
    resolve(videoUrl)
  }, [videoUrl, resolve])

  const handleApply = () => {
    const url = extractUrlFromText(input)
    setInput(url || input.trim())
    onVideoUrlChange(url || input.trim())
    resolve(input)
  }

  const showDouyinFallback =
    !loading &&
    parsed &&
    (parsed.type === 'douyin' || resolveHint) &&
    (!parsed.embedUrl || embedFailed)

  return (
    <section className="panel video-panel">
      <div className="panel-header">
        <h2>视频学习</h2>
        <span className="panel-hint">支持整段分享文案 / 抖音 / B站 / YouTube</span>
      </div>

      <div className="video-input-row">
        <input
          className="video-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleApply()}
          placeholder="粘贴分享文案或视频链接..."
        />
        <button type="button" className="btn-secondary" onClick={handleApply} disabled={loading}>
          {loading ? '解析中' : '加载'}
        </button>
      </div>

      <div className="video-stage">
        {loading && <div className="video-placeholder">正在解析链接…</div>}

        {!loading && parsed?.type === 'direct' && parsed.src && (
          <video ref={videoRef} className="video-player" src={parsed.src} controls playsInline />
        )}

        {!loading && parsed?.embedUrl && !embedFailed && !showDouyinFallback && (
          <iframe
            className="video-embed"
            src={parsed.embedUrl}
            title="视频播放"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            onError={() => setEmbedFailed(true)}
          />
        )}

        {showDouyinFallback && (
          <div className="video-fallback">
            <p>{resolveHint || '该视频无法内嵌播放（平台限制）'}</p>
            <a
              href={parsed?.originalUrl || extractUrlFromText(input)}
              target="_blank"
              rel="noreferrer"
              className="link-btn"
            >
              在浏览器打开视频
            </a>
            <p className="video-tip">耳机模式：开始传译后选择「分享此标签页」并勾选音频</p>
          </div>
        )}

        {!loading && parsed?.type === 'unknown' && parsed.originalUrl && !showDouyinFallback && (
          <div className="video-fallback">
            <p>{resolveHint || '无法识别该链接格式'}</p>
            <a href={parsed.originalUrl} target="_blank" rel="noreferrer" className="link-btn">
              打开原链接
            </a>
          </div>
        )}

        {!loading && !parsed?.originalUrl && (
          <div className="video-placeholder">
            <div className="video-placeholder-icon">▶</div>
            <p>粘贴演讲视频链接开始学习</p>
            <p className="video-tip">可直接粘贴抖音分享文案，自动提取链接</p>
          </div>
        )}
      </div>
    </section>
  )
}
