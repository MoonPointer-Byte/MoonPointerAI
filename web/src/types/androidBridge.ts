export interface MoonPointerAndroidBridge {
  getServerUrl?(): string
  getWebUrl?(): string
  isAndroidApp?(): boolean
  getTheme?(): string
  setTheme?(mode: string): void
  speakTts?(text: string, lang: string, rate: number, utteranceId: string): void
  stopTts?(): void
}

declare global {
  interface Window {
    MoonPointerAndroid?: MoonPointerAndroidBridge
    __moonTtsDone?: (utteranceId: string) => void
  }
}

export {}
