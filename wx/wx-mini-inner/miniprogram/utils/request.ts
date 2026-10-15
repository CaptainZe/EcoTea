import { getApiBaseUrl } from '../config/env'
import { getToken } from '../services/token'

/** 与 server-api ApiResult 对齐 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: WechatMiniprogram.IAnyObject | string | ArrayBuffer
  /** 默认 true：自动带 X-Employee-Token（有则带） */
  auth?: boolean
  header?: Record<string, string>
}

/**
 * 统一请求封装。成功时 resolve 整个 ApiResult；网络失败 reject。
 * 业务码非 0 仍 resolve（由调用方判断 code），便于展示 message。
 */
export function request<T = unknown>(options: RequestOptions): Promise<ApiResult<T>> {
  const method = options.method || 'GET'
  const auth = options.auth !== false
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.header || {}),
  }
  if (auth) {
    const token = getToken()
    if (token) {
      header['X-Employee-Token'] = token
    }
  }

  const fullUrl = options.url.startsWith('http')
    ? options.url
    : getApiBaseUrl().replace(/\/$/, '') + (options.url.startsWith('/') ? options.url : '/' + options.url)

  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl,
      method,
      data: options.data,
      header,
      success(res) {
        const status = res.statusCode || 0
        const body = res.data as ApiResult<T>
        if (status === 401) {
          const msg =
            body && typeof body === 'object' && body.message
              ? String(body.message)
              : '登录已失效'
          reject(new Error(msg))
          return
        }
        if (status < 200 || status >= 300) {
          reject(new Error('HTTP ' + status))
          return
        }
        if (body == null || typeof body !== 'object' || typeof (body as ApiResult).code !== 'number') {
          reject(new Error('响应格式无效'))
          return
        }
        resolve(body)
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || '网络请求失败'))
      },
    })
  })
}
