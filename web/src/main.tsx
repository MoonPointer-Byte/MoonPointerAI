import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { initTheme } from './hooks/useTheme'
import { initTtsEngine } from './utils/ttsEngine'
import './index.css'
import './theme.css'

initTheme()
initTtsEngine()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
