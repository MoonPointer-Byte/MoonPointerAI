import { unlockWebTts } from '../utils/ttsEngine'

interface TtsPaceToggleProps {
  ttsEnabled: boolean
  matchSource: boolean
  onChange: (next: { ttsEnabled: boolean; ttsMatchSource: boolean }) => void
}

export function TtsPaceToggle({ ttsEnabled, matchSource, onChange }: TtsPaceToggleProps) {
  const handleClick = () => {
    unlockWebTts()
    if (!ttsEnabled) {
      onChange({ ttsEnabled: true, ttsMatchSource: false })
      return
    }
    onChange({ ttsEnabled: true, ttsMatchSource: !matchSource })
  }

  const label = !ttsEnabled ? '播报' : matchSource ? '跟读' : '快播'

  return (
    <button
      type="button"
      className={`tts-pace-btn ${ttsEnabled ? (matchSource ? 'active' : 'on') : ''}`}
      onClick={handleClick}
      aria-pressed={ttsEnabled && matchSource}
      title={
        !ttsEnabled
          ? '开启语音播报（默认快速语速）'
          : matchSource
            ? '跟读模式：匹配音源语速（点击切回快播）'
            : '快播模式：较快易懂（点击切换跟读）'
      }
    >
      {label}
    </button>
  )
}
