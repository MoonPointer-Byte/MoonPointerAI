import { useCallback, useEffect, useRef, useState } from 'react'
import type { WsMessage } from '../types'

function getWsUrl(serverUrl: string): string {
  const url = new URL(serverUrl)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws/translate'
  return url.toString()
}

export function useWebSocket(serverUrl: string, onMessage: (msg: WsMessage) => void) {
  const [connected, setConnected] = useState(false)
  const [llmConfigured, setLlmConfigured] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return

    const ws = new WebSocket(getWsUrl(serverUrl))
    wsRef.current = ws

    ws.onopen = () => setConnected(true)
    ws.onclose = () => setConnected(false)
    ws.onerror = () => setConnected(false)
    ws.onmessage = (event) => {
      const msg: WsMessage = JSON.parse(event.data)
      if (msg.type === 'STATUS') {
        setLlmConfigured(msg.message !== 'connected_no_llm')
      }
      onMessageRef.current(msg)
    }
  }, [serverUrl])

  const disconnect = useCallback(() => {
    wsRef.current?.close()
    wsRef.current = null
    setConnected(false)
  }, [])

  const send = useCallback((msg: WsMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg))
    }
  }, [])

  useEffect(() => {
    return () => {
      wsRef.current?.close()
    }
  }, [])

  return { connected, llmConfigured, connect, disconnect, send }
}
