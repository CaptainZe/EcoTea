import { request, ApiResult } from '../utils/request'
import { clearToken, setToken } from './token'

export interface TenantOption {
  tenantId: number
  name: string
  status: number
}

export interface EmployeeLoginData {
  token: string
  employeeId: number
  username: string
  realName: string
  tenantSelected: boolean
  tenantId?: number
  tenantName?: string
  permCodes?: string[]
  tenants?: TenantOption[]
}

export interface EmployeeSessionData {
  employeeId: number
  username: string
  realName: string
  tenantSelected: boolean
  tenantId?: number
  tenantName?: string
  permCodes?: string[]
  wxBound?: boolean
}

const TENANTS_CACHE_KEY = 'employee_tenants_cache'
const PROFILE_CACHE_KEY = 'employee_profile_cache'
const CURRENT_TENANT_KEY = 'employee_current_tenant'

function applyLoginLocal(data: EmployeeLoginData): void {
  setToken(data.token)
  try {
    wx.setStorageSync(TENANTS_CACHE_KEY, data.tenants || [])
    wx.setStorageSync(PROFILE_CACHE_KEY, {
      employeeId: data.employeeId,
      username: data.username,
      realName: data.realName,
    })
    wx.removeStorageSync(CURRENT_TENANT_KEY)
  } catch (e) {
    // ignore
  }
}

export function login(username: string, password: string): Promise<ApiResult<EmployeeLoginData>> {
  return request<EmployeeLoginData>({
    url: '/employee/login',
    method: 'POST',
    auth: false,
    data: { username, password },
  }).then((res) => {
    if (res.code === 0 && res.data && res.data.token) {
      applyLoginLocal(res.data)
    }
    return res
  })
}

/** 微信一键登录（需已绑定）；成功后本地写入同账密登录 */
export function wxLogin(code: string): Promise<ApiResult<EmployeeLoginData>> {
  return request<EmployeeLoginData>({
    url: '/employee/wx-login',
    method: 'POST',
    auth: false,
    data: { code },
  }).then((res) => {
    if (res.code === 0 && res.data && res.data.token) {
      applyLoginLocal(res.data)
    }
    return res
  })
}

export function getCachedTenants(): TenantOption[] {
  try {
    return wx.getStorageSync(TENANTS_CACHE_KEY) || []
  } catch (e) {
    return []
  }
}

export function fetchTenants(): Promise<ApiResult<TenantOption[]>> {
  return request<TenantOption[]>({
    url: '/employee/tenants',
    method: 'GET',
    auth: true,
  }).then((res) => {
    if (res.code === 0 && res.data) {
      try {
        wx.setStorageSync(TENANTS_CACHE_KEY, res.data)
      } catch (e) {
        // ignore
      }
    }
    return res
  })
}

export function selectTenant(tenantId: number): Promise<ApiResult<EmployeeSessionData>> {
  return request<EmployeeSessionData>({
    url: '/employee/select-tenant',
    method: 'POST',
    auth: true,
    data: { tenantId },
  }).then((res) => {
    if (res.code === 0 && res.data && res.data.tenantSelected) {
      syncSessionToLocal(res.data)
    }
    return res
  })
}

export function fetchSession(): Promise<ApiResult<EmployeeSessionData>> {
  return request<EmployeeSessionData>({
    url: '/employee/session',
    method: 'GET',
    auth: true,
  })
}

export function syncSessionToLocal(session: EmployeeSessionData): void {
  try {
    wx.setStorageSync(PROFILE_CACHE_KEY, {
      employeeId: session.employeeId,
      username: session.username,
      realName: session.realName,
    })
    if (session.tenantSelected && session.tenantId) {
      wx.setStorageSync(CURRENT_TENANT_KEY, {
        tenantId: session.tenantId,
        tenantName: session.tenantName,
        permCodes: session.permCodes || [],
      })
    } else {
      wx.removeStorageSync(CURRENT_TENANT_KEY)
    }
  } catch (e) {
    // ignore
  }
}

export function getCurrentTenant(): { tenantId?: number; tenantName?: string; permCodes?: string[] } | null {
  try {
    return wx.getStorageSync(CURRENT_TENANT_KEY) || null
  } catch (e) {
    return null
  }
}

/** 本地缓存的员工资料（登录/选店写入） */
export function getCachedProfile(): {
  employeeId?: number
  username?: string
  realName?: string
} | null {
  try {
    return wx.getStorageSync(PROFILE_CACHE_KEY) || null
  } catch (e) {
    return null
  }
}

/** 报单人展示名：真实姓名优先，否则登录名 */
export function getReporterName(): string {
  const p = getCachedProfile()
  if (!p) return ''
  const name = (p.realName || '').trim()
  if (name) return name
  return (p.username || '').trim()
}

/** 清掉登录附属缓存（不含 token 时用 clearToken 另清） */
export function clearLocalAuthExtras(): void {
  try {
    wx.removeStorageSync(TENANTS_CACHE_KEY)
    wx.removeStorageSync(PROFILE_CACHE_KEY)
    wx.removeStorageSync(CURRENT_TENANT_KEY)
  } catch (e) {
    // ignore
  }
}

export function clearAllAuthLocal(): void {
  clearToken()
  clearLocalAuthExtras()
}

/** 服务端注销；失败由调用方仍清本地 */
export function logout(): Promise<ApiResult<null>> {
  return request<null>({
    url: '/employee/logout',
    method: 'POST',
    auth: true,
  })
}

/** 自助改密；成功后服务端踢会话，调用方须清本地并回登录页 */
export function changePassword(
  oldPassword: string,
  newPassword: string
): Promise<ApiResult<null>> {
  return request<null>({
    url: '/employee/change-password',
    method: 'POST',
    auth: true,
    data: { oldPassword, newPassword },
  })
}

/** 小程序 wx.login 取临时 code */
export function getWxLoginCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res && res.code) {
          resolve(res.code)
          return
        }
        reject(new Error('获取微信登录码失败'))
      },
      fail() {
        reject(new Error('获取微信登录码失败'))
      },
    })
  })
}

/** 绑定当前微信到已登录员工 */
export function wxBind(code: string): Promise<ApiResult<EmployeeSessionData>> {
  return request<EmployeeSessionData>({
    url: '/employee/wx-bind',
    method: 'POST',
    auth: true,
    data: { code },
  })
}

/** 解绑微信 */
export function wxUnbind(): Promise<ApiResult<EmployeeSessionData>> {
  return request<EmployeeSessionData>({
    url: '/employee/wx-unbind',
    method: 'POST',
    auth: true,
  })
}
