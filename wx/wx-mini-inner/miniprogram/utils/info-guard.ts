import { getCurrentTenant } from '../services/auth-api'

/** 茶品信息页权限：需已选店且含 INFO。 */
export function assertInfoAccess(): boolean {
  const tenant = getCurrentTenant()
  const perms = (tenant && tenant.permCodes) || []
  if (!tenant || !tenant.tenantId) {
    wx.showToast({ title: '请先选择店铺', icon: 'none' })
    setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
    return false
  }
  if (perms.indexOf('INFO') < 0) {
    wx.showToast({ title: '无信息管理权限', icon: 'none' })
    setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
    return false
  }
  return true
}
