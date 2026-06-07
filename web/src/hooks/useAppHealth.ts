import { useEffect, useState } from 'react'

export function useAppHealth(serverUrl: string) {
  const [sttConfigured, setSttConfigured] = useState(false)

  useEffect(() => {
    const base = serverUrl.replace(/\/$/, '')
    fetch(`${base}/api/health`)
      .then((r) => r.json())
      .then((data) => setSttConfigured(!!data.sttConfigured))
      .catch(() => setSttConfigured(false))
  }, [serverUrl])

  return { sttConfigured }
}
