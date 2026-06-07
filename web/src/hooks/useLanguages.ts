import { useEffect, useState } from 'react'
import { FALLBACK_LANGUAGES, type LanguageOption } from '../constants/languages'

export function useLanguages(serverUrl: string) {
  const [languages, setLanguages] = useState<LanguageOption[]>(FALLBACK_LANGUAGES)

  useEffect(() => {
    const base = serverUrl.replace(/\/$/, '')
    fetch(`${base}/api/languages`)
      .then((r) => r.json())
      .then((data) => {
        const codes: string[] = data.source ?? []
        const labels: Record<string, string> = data.labels ?? {}
        const speechTags: Record<string, string> = data.speechTags ?? {}
        if (codes.length) {
          setLanguages(
            codes.map((code) => ({
              code,
              label: labels[code] ?? code,
              speechTag: speechTags[code] ?? 'en-US',
            }))
          )
        }
      })
      .catch(() => { /* use fallback */ })
  }, [serverUrl])

  return languages
}
