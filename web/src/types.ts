export type WsMessageType =

  | 'CONFIG'

  | 'SPEECH'

  | 'TRANSLATION'

  | 'CORRECTION'

  | 'ERROR'

  | 'STATUS'

  | 'PING'

  | 'PONG'



export interface WsMessage {

  type: WsMessageType

  sessionId?: string

  segmentId?: string

  text?: string

  translatedText?: string

  sourceLang?: string

  targetLang?: string

  isFinal?: boolean

  corrected?: boolean

  message?: string

  ttsEnabled?: boolean

}



export interface SubtitleSegment {

  id: string

  sourceText: string

  translatedText: string

  isFinal: boolean

  corrected: boolean

  timestampMs?: number

}



export type AudioInputMode = 'mic' | 'headphone'

export interface AppConfig {
  serverUrl: string
  sourceLang: string
  targetLang: string
  audioInput: AudioInputMode
  ttsEnabled: boolean
  /** false=快速易懂语速（默认），true=匹配音源语速 */
  ttsMatchSource: boolean
  showSource: boolean
  autoScroll: boolean
  showTranslation: boolean
}


