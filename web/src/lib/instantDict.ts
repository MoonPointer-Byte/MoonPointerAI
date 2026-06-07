// 常用词本地即时翻译，0 延迟
const EN_ZH: Record<string, string> = {
  hello: '你好', hi: '你好', hey: '嘿',
  yes: '是的', no: '不', ok: '好的', okay: '好的',
  thanks: '谢谢', thank: '感谢', please: '请', sorry: '对不起',
  good: '好', bad: '坏', great: '很棒', nice: '很好',
  morning: '早上', afternoon: '下午', evening: '晚上', night: '晚上',
  today: '今天', tomorrow: '明天', yesterday: '昨天',
  i: '我', you: '你', we: '我们', they: '他们', he: '他', she: '她', it: '它',
  is: '是', are: '是', am: '是', was: '曾是', were: '曾是',
  have: '有', has: '有', do: '做', does: '做', can: '能', will: '将',
  the: '', a: '一个', an: '一个',
  and: '和', or: '或', but: '但是', so: '所以', because: '因为',
  what: '什么', when: '什么时候', where: '哪里', who: '谁', why: '为什么', how: '怎么',
  this: '这个', that: '那个', here: '这里', there: '那里',
  go: '去', come: '来', see: '看', know: '知道', think: '想', want: '想要',
  make: '做', get: '得到', give: '给', take: '拿', say: '说', tell: '告诉',
  like: '喜欢', love: '爱', need: '需要', use: '使用', work: '工作',
  time: '时间', year: '年', day: '天', way: '方式', man: '男人', woman: '女人',
  world: '世界', life: '生活', hand: '手', part: '部分', place: '地方',
  case: '情况', week: '周', company: '公司', system: '系统', program: '程序',
  question: '问题', government: '政府', number: '数字',
  point: '点', home: '家', water: '水', room: '房间', mother: '母亲', father: '父亲',
  money: '钱', story: '故事', fact: '事实', month: '月', lot: '很多',
  right: '对', left: '左', new: '新', old: '旧', big: '大', small: '小',
  long: '长', short: '短', high: '高', low: '低', first: '第一', last: '最后',
  next: '下一个', same: '相同', different: '不同', important: '重要',
  very: '非常', really: '真的', just: '只是', also: '也', only: '只有',
  well: '好', now: '现在', then: '然后', still: '仍然', already: '已经',
  always: '总是', never: '从不', maybe: '也许', sure: '当然',
  welcome: '欢迎', goodbye: '再见', bye: '再见',
  name: '名字', help: '帮助', start: '开始', stop: '停止', end: '结束',
  open: '打开', close: '关闭', read: '读', write: '写', listen: '听', speak: '说',
  learn: '学习', teach: '教', understand: '理解', mean: '意思是',
  english: '英语', chinese: '中文', language: '语言', word: '词', sentence: '句子',
  technology: '技术', computer: '电脑', software: '软件', data: '数据',
  network: '网络', internet: '互联网', phone: '手机', email: '邮件',
  meeting: '会议', project: '项目', team: '团队', idea: '想法', example: '例子',
  problem: '问题', solution: '解决方案', result: '结果', change: '改变',
  develop: '开发', development: '开发', design: '设计', test: '测试',
  user: '用户', server: '服务器', client: '客户端', code: '代码', bug: '漏洞',
  true: '真', false: '假', zero: '零', one: '一', two: '二', three: '三',
  four: '四', five: '五', six: '六', seven: '七', eight: '八', nine: '九', ten: '十',
}

export function lookupInstant(text: string, sourceLang: string, targetLang: string): string | null {
  if (sourceLang !== 'en' || targetLang !== 'zh') return null
  const key = text.trim().toLowerCase().replace(/[.,!?;:'"]/g, '')
  if (!key) return null
  if (EN_ZH[key] !== undefined) return EN_ZH[key] || null
  return null
}

export function lookupLastWord(text: string, sourceLang: string, targetLang: string): string | null {
  const words = text.trim().split(/\s+/)
  if (words.length === 0) return null
  const last = words[words.length - 1].toLowerCase().replace(/[.,!?;:'"]/g, '')
  return lookupInstant(last, sourceLang, targetLang)
}

export function translatePhraseLocally(text: string, sourceLang: string, targetLang: string): string | null {
  if (sourceLang !== 'en' || targetLang !== 'zh') return null
  const words = text.trim().toLowerCase().split(/\s+/)
  const parts: string[] = []
  for (const w of words) {
    const clean = w.replace(/[.,!?;:'"]/g, '')
    if (!clean) continue
    const t = EN_ZH[clean]
    if (t === undefined) return null
    if (t) parts.push(t)
  }
  return parts.length > 0 ? parts.join('') : null
}
