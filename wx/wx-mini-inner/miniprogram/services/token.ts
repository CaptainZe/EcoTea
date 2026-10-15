/** 员工会话 token 本地存取（E1.2） */

const TOKEN_KEY = 'employee_token'

export function getToken(): string {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setToken(token: string): void {
  wx.setStorageSync(TOKEN_KEY, token || '')
}

export function clearToken(): void {
  try {
    wx.removeStorageSync(TOKEN_KEY)
  } catch (e) {
    // ignore
  }
}
