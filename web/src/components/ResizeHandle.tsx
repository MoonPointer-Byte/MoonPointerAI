interface ResizeHandleProps {
  axis: 'column' | 'row'
  onMouseDown: (e: React.MouseEvent) => void
  label?: string
}

export function ResizeHandle({ axis, onMouseDown, label }: ResizeHandleProps) {
  return (
    <div
      className={`resize-handle resize-handle--${axis}`}
      onMouseDown={onMouseDown}
      role="separator"
      aria-orientation={axis === 'column' ? 'vertical' : 'horizontal'}
      aria-label={label ?? '拖动调节尺寸'}
      title={label ?? '拖动调节尺寸'}
    />
  )
}
