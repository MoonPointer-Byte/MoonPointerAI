import { randomId } from './randomId'

const KEY = 'translator-client-session'

export function getClientSessionId(): string {
  let id = localStorage.getItem(KEY)
  if (!id) {
    id = randomId()
    localStorage.setItem(KEY, id)
  }
  return id
}
