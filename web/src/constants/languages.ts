export interface LanguageOption {
  code: string
  label: string
  speechTag: string
}

export const FALLBACK_LANGUAGES: LanguageOption[] = [
  { code: 'zh', label: '中文', speechTag: 'zh-CN' },
  { code: 'en', label: '英语', speechTag: 'en-US' },
  { code: 'ja', label: '日语', speechTag: 'ja-JP' },
  { code: 'ko', label: '韩语', speechTag: 'ko-KR' },
  { code: 'fr', label: '法语', speechTag: 'fr-FR' },
  { code: 'de', label: '德语', speechTag: 'de-DE' },
  { code: 'es', label: '西班牙语', speechTag: 'es-ES' },
  { code: 'ru', label: '俄语', speechTag: 'ru-RU' },
  { code: 'pt', label: '葡萄牙语', speechTag: 'pt-BR' },
  { code: 'it', label: '意大利语', speechTag: 'it-IT' },
  { code: 'ar', label: '阿拉伯语', speechTag: 'ar-SA' },
  { code: 'th', label: '泰语', speechTag: 'th-TH' },
  { code: 'vi', label: '越南语', speechTag: 'vi-VN' },
  { code: 'id', label: '印尼语', speechTag: 'id-ID' },
  { code: 'ms', label: '马来语', speechTag: 'ms-MY' },
  { code: 'hi', label: '印地语', speechTag: 'hi-IN' },
  { code: 'nl', label: '荷兰语', speechTag: 'nl-NL' },
  { code: 'pl', label: '波兰语', speechTag: 'pl-PL' },
  { code: 'tr', label: '土耳其语', speechTag: 'tr-TR' },
  { code: 'sv', label: '瑞典语', speechTag: 'sv-SE' },
]

export function labelFor(code: string, languages: LanguageOption[]): string {
  return languages.find((l) => l.code === code)?.label ?? code
}

export function speechTagFor(code: string, languages: LanguageOption[]): string {
  return languages.find((l) => l.code === code)?.speechTag ?? 'en-US'
}
