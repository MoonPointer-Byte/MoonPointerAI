import type { LayoutPrefs } from '../hooks/useLayoutPrefs'
import { DEFAULT_LAYOUT } from '../hooks/useLayoutPrefs'

interface LayoutSettingsProps {
  layout: LayoutPrefs
  onChange: (patch: Partial<LayoutPrefs>) => void
  onReset: () => void
}

function SliderRow({
  label,
  value,
  min,
  max,
  unit,
  onChange,
}: {
  label: string
  value: number
  min: number
  max: number
  unit: string
  onChange: (v: number) => void
}) {
  return (
    <label className="layout-slider">
      <span className="layout-slider-label">
        {label}
        <em>{value}{unit}</em>
      </span>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    </label>
  )
}

export function LayoutSettings({ layout, onChange, onReset }: LayoutSettingsProps) {
  return (
    <div className="layout-settings">
      <div className="layout-settings-head">
        <span className="layout-settings-title">界面布局</span>
        <button type="button" className="layout-reset-btn" onClick={onReset}>
          恢复默认
        </button>
      </div>
      <div className="layout-settings-grid">
        <SliderRow
          label="侧栏宽度"
          value={layout.sideWidthPercent}
          min={24}
          max={60}
          unit="%"
          onChange={(v) => onChange({ sideWidthPercent: v })}
        />
        <SliderRow
          label="视频区高度"
          value={layout.videoSplitPercent}
          min={25}
          max={75}
          unit="%"
          onChange={(v) => onChange({ videoSplitPercent: v })}
        />
        <SliderRow
          label="圆角"
          value={layout.panelRadius}
          min={6}
          max={28}
          unit="px"
          onChange={(v) => onChange({ panelRadius: v })}
        />
        <SliderRow
          label="边框粗细"
          value={layout.borderWidth}
          min={0}
          max={3}
          unit="px"
          onChange={(v) => onChange({ borderWidth: v })}
        />
        <SliderRow
          label="模块间距"
          value={layout.panelGap}
          min={4}
          max={24}
          unit="px"
          onChange={(v) => onChange({ panelGap: v })}
        />
        <SliderRow
          label="外边距"
          value={layout.workspacePadding}
          min={8}
          max={28}
          unit="px"
          onChange={(v) => onChange({ workspacePadding: v })}
        />
      </div>
      <p className="layout-settings-hint">
        也可直接拖动模块之间的分隔条调节尺寸 · 默认 {DEFAULT_LAYOUT.sideWidthPercent}% / {DEFAULT_LAYOUT.videoSplitPercent}%
      </p>
    </div>
  )
}
