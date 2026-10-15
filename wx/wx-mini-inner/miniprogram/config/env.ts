/**
 * API 环境配置（E1.1）
 * - 开发默认走局域网本地；上线前改为 prod
 * - 真机预览需手机与开发机同一局域网，且防火墙放行 8081
 */
export type ApiEnv = 'dev' | 'prod'

const API_BASE: Record<ApiEnv, string> = {
  dev: 'http://10.15.129.22:8081',
  prod: 'https://api.ecotea.cn',
}

/** 当前环境：开发阶段用 dev；正式发布改为 prod */
export const CURRENT_ENV: ApiEnv = 'dev'

export function getApiBaseUrl(): string {
  return API_BASE[CURRENT_ENV]
}
