import { MoonPointerLogo } from './MoonPointerLogo'

interface EmptyStateProps {
  targetLabel: string
  onStart: () => void
  disabled: boolean
}

export function EmptyState({ targetLabel, onStart, disabled }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <div className="empty-hero">
        <div className="empty-glow" aria-hidden />
        <div className="empty-orbit" aria-hidden />
        <div className="empty-logo-wrap">
          <MoonPointerLogo size={76} />
        </div>

        <p className="empty-brand">MoonPointer</p>
        <p className="empty-tagline">同声传译 · 视频学习</p>

        <p className="empty-desc">
          <span className="empty-desc-side">
            在右侧加载视频并播放，系统将实时识别语音，
            翻译为 <strong>{targetLabel}</strong> 字幕。
          </span>
          <span className="empty-desc-below">
            在下方加载视频并播放，系统将实时识别语音，
            翻译为 <strong>{targetLabel}</strong> 字幕。
          </span>
        </p>

        <div className="empty-features" aria-label="功能">
          <span className="empty-feature">实时字幕</span>
          <span className="empty-feature">多语翻译</span>
          <span className="empty-feature">云端笔记</span>
        </div>

        <button
          type="button"
          className="start-btn start-btn-hero"
          onClick={onStart}
          disabled={disabled}
        >
          <span className="start-btn-shine" aria-hidden />
          开始传译
        </button>

        <ol className="empty-steps">
          <li><span>01</span>加载视频</li>
          <li><span>02</span>开始传译</li>
          <li><span>03</span>实时学习</li>
        </ol>
      </div>
    </div>
  )
}
