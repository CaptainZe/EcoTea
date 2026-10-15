import { clearToken, getToken } from './token'
import {
  EmployeeSessionData,
  fetchSession,
  getCurrentTenant,
  clearLocalAuthExtras,
  syncSessionToLocal,
} from './auth-api'

export type LaunchTarget =
  | '/pages/login/login'
  | '/pages/select-tenant/select-tenant'
  | '/pages/index/index'

/**
 * 根据本地 token + 服务端 session 决定启动落地页。
 */
export function resolveLaunchTarget(): Promise<LaunchTarget> {
  const token = getToken()
  if (!token) {
    clearLocalAuthExtras()
    return Promise.resolve('/pages/login/login')
  }

  return fetchSession()
    .then((res) => {
      if (res.code !== 0 || !res.data) {
        clearToken()
        clearLocalAuthExtras()
        return '/pages/login/login' as LaunchTarget
      }
      const session = res.data
      syncSessionToLocal(session)
      if (!session.tenantSelected || !session.tenantId) {
        return '/pages/select-tenant/select-tenant' as LaunchTarget
      }
      return '/pages/index/index' as LaunchTarget
    })
    .catch((err) => {
      console.warn('[E1.5] session check fail', err)
      clearToken()
      clearLocalAuthExtras()
      return '/pages/login/login' as LaunchTarget
    })
}

export function reLaunchTo(url: LaunchTarget | string): void {
  // 首页是 tabBar 页；login / select-tenant 不是
  if (url === '/pages/index/index') {
    wx.switchTab({
      url,
      fail: () => {
        wx.reLaunch({ url })
      },
    })
    return
  }
  wx.reLaunch({ url })
}

/** 本地是否已具备「已选店」缓存（仅作展示；权威以 session 为准） */
export function hasLocalTenantSelected(): boolean {
  const t = getCurrentTenant()
  return !!(t && t.tenantId)
}

export type { EmployeeSessionData }
