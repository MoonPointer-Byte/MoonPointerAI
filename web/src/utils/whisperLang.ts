/** Whisper API 语言代码（ISO-639-1） */
export function whisperLangCode(langCode: string): string {
  const map: Record<string, string> = {
    zh: 'zh',
    en: 'en',
    ja: 'ja',
    ko: 'ko',
    fr: 'fr',
    de: 'de',
    es: 'es',
    ru: 'ru',
    pt: 'pt',
    it: 'it',
    ar: 'ar',
    th: 'th',
    vi: 'vi',
    id: 'id',
    ms: 'ms',
    hi: 'hi',
    nl: 'nl',
    pl: 'pl',
    tr: 'tr',
    sv: 'sv',
  }
  return map[langCode] ?? langCode.slice(0, 2)
}
