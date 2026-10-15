import { getCurrentTenant } from '../services/auth-api'

/**
 * 回收价目 / 报价页权限：需已选店且含 RECYCLE。
 * 失败时 toast 并尝试返回。
 */
export function assertRecycleAccess(): boolean {
  const tenant = getCurrentTenant()
  const perms = (tenant && tenant.permCodes) || []
  if (!tenant || !tenant.tenantId) {
    wx.showToast({ title: '请先选择店铺', icon: 'none' })
    setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
    return false
  }
  if (perms.indexOf('RECYCLE') < 0) {
    wx.showToast({ title: '无回收权限', icon: 'none' })
    setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
    return false
  }
  return true
}
