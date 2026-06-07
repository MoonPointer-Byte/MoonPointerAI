/** 从分享文案中提取第一个 http(s) 链接 */
export function extractUrlFromText(input: string): string {
  const trimmed = input.trim()
  const match = trimmed.match(/https?:\/\/[^\s\u4e00-\u9fff]+/i)
  return match ? match[0].replace(/[.,;:!?）)]+$/, '') : trimmed
}
