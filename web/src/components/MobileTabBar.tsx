export type MobileTab = 'transcript' | 'video' | 'notes'

interface MobileTabBarProps {
  active: MobileTab
  onChange: (tab: MobileTab) => void
}

const TABS: { id: MobileTab; label: string }[] = [
  { id: 'transcript', label: '字幕' },
  { id: 'video', label: '视频' },
  { id: 'notes', label: '笔记' },
]

export function MobileTabBar({ active, onChange }: MobileTabBarProps) {
  return (
    <nav className="mobile-tab-bar" aria-label="主功能区">
      {TABS.map((tab) => (
        <button
          key={tab.id}
          type="button"
          className={`mobile-tab-btn ${active === tab.id ? 'active' : ''}`}
          onClick={() => onChange(tab.id)}
          aria-current={active === tab.id ? 'page' : undefined}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  )
}
