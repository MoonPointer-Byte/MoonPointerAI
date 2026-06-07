import { extractUrlFromText } from './extractUrl'

export type VideoType = 'youtube' | 'bilibili' | 'douyin' | 'direct' | 'unknown' | 'link'

export interface ParsedVideo {
  type: VideoType
  embedUrl?: string
  src?: string
  originalUrl: string
  videoId?: string
}

export function parseVideoUrl(input: string): ParsedVideo {
  const url = extractUrlFromText(input)
  if (!url) {
    return { type: 'unknown', originalUrl: '' }
  }

  const ytMatch = url.match(
    /(?:youtube\.com\/(?:watch\?v=|embed\/|shorts\/)|youtu\.be\/)([\w-]{11})/
  )
  if (ytMatch) {
    return {
      type: 'youtube',
      videoId: ytMatch[1],
      embedUrl: `https://www.youtube.com/embed/${ytMatch[1]}?rel=0`,
      originalUrl: url,
    }
  }

  const biliMatch = url.match(/bilibili\.com\/video\/(BV[\w]+)/i)
  if (biliMatch) {
    return {
      type: 'bilibili',
      videoId: biliMatch[1],
      embedUrl: `https://player.bilibili.com/player.html?bvid=${biliMatch[1]}&high_quality=1`,
      originalUrl: url,
    }
  }

  const douyinMatch = url.match(/douyin\.com\/(?:video|light)\/(\d+)/)
  if (douyinMatch) {
    return {
      type: 'douyin',
      videoId: douyinMatch[1],
      embedUrl: `https://www.douyin.com/light/${douyinMatch[1]}`,
      originalUrl: url,
    }
  }

  if (/\.(mp4|webm|ogg)(\?|$)/i.test(url)) {
    return { type: 'direct', src: url, originalUrl: url }
  }

  if (url.includes('douyin.com') || url.includes('iesdouyin.com')) {
    return { type: 'douyin', originalUrl: url }
  }

  return { type: 'unknown', originalUrl: url }
}
