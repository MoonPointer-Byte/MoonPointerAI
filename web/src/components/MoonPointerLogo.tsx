interface MoonPointerLogoProps {
  size?: number
  className?: string
}

export function MoonPointerLogo({ size = 42, className = '' }: MoonPointerLogoProps) {
  return (
    <svg
      className={`moon-logo ${className}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      <defs>
        <linearGradient id="mp-bg" x1="8" y1="6" x2="92" y2="94" gradientUnits="userSpaceOnUse">
          <stop stopColor="#312e81" />
          <stop offset="1" stopColor="#0a0a12" />
        </linearGradient>
        <linearGradient id="mp-moon" x1="28" y1="28" x2="58" y2="72" gradientUnits="userSpaceOnUse">
          <stop stopColor="#f8fafc" />
          <stop offset="1" stopColor="#a5b4fc" />
        </linearGradient>
        <linearGradient id="mp-beam" x1="58" y1="64" x2="84" y2="76" gradientUnits="userSpaceOnUse">
          <stop stopColor="#67e8f9" />
          <stop offset="1" stopColor="#818cf8" />
        </linearGradient>
      </defs>
      <rect width="100" height="100" rx="22" fill="url(#mp-bg)" />
      <path fill="url(#mp-moon)" d="M44 26a26 26 0 1 1-2.2 51.8A20 20 0 1 0 44 26z" />
      <path stroke="url(#mp-beam)" strokeWidth="4.5" strokeLinecap="round" d="M58 58 L76 68" />
      <circle cx="79" cy="70" r="5.5" fill="#67e8f9" />
      <circle cx="79" cy="70" r="2" fill="#f0fdfa" />
    </svg>
  )
}
