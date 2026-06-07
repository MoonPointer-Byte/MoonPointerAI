import '../types/androidBridge'

export function isAndroidApp(): boolean {
  if (typeof window === 'undefined') return false
  return window.MoonPointerAndroid?.isAndroidApp?.() === true
}