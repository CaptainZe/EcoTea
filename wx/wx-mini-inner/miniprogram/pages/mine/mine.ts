import {
  clearAllAuthLocal,
  fetchSession,
  getCurrentTenant,
  getWxLoginCode,
  logout,
  syncSessionToLocal,
  wxBind,
  wxUnbind,
} from '../../services/auth-api'
import { reLaunchTo } from '../../services/session'
import { COLOR_DANGER } from '../../styles/token-colors'

Page({
  data: {
    username: '',
    realName: '',
    tenantName: '',
    wxBound: false,
    wxBusy: false,
    loggingOut: false,
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    let username = ''
    let realName = ''
    try {
      const profile = wx.getStorageSync('employee_profile_cache') || {}
      username = profile.username || ''
      realName = profile.realName || ''
    } catch (e) {
      // ignore
    }
    const tenant = getCurrentTenant()
    this.setData({
      username,
      realName,
      tenantName: (tenant && tenant.tenantName) || '',
    })
    fetchSession()
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          return
        }
        syncSessionToLocal(res.data)
        this.setData({
          username: res.data.username || username,
          realName: res.data.realName || realName,
          tenantName:
            (res.data.tenantSelected && res.data.tenantName) ||
            (tenant && tenant.tenantName) ||
            '',
          wxBound: !!res.data.wxBound,
        })
      })
      .catch(() => {
        // 忽略，保留本地展示
      })
  },

  onWxBind() {
    if (this.data.wxBusy || this.data.wxBound) return
    this.setData({ wxBusy: true })
    getWxLoginCode()
      .then((code) => wxBind(code))
      .then((res) => {
        this.setData({ wxBusy: false })
        if (res.code !== 0 || !res.data) {
          wx.showToast({ title: res.message || '绑定失败', icon: 'none' })
          return
        }
        this.setData({ wxBound: !!res.data.wxBound })
        wx.showToast({ title: '绑定成功', icon: 'success' })
      })
      .catch((err: Error) => {
        this.setData({ wxBusy: false })
        wx.showToast({ title: (err && err.message) || '绑定失败', icon: 'none' })
      })
  },

  onWxUnbind() {
    if (this.data.wxBusy || !this.data.wxBound) return
    wx.showModal({
      title: '解绑微信',
      content: '解绑后无法使用微信一键登录，确定解绑？',
      confirmText: '解绑',
      confirmColor: COLOR_DANGER,
      success: (modal) => {
        if (!modal.confirm) return
        this.setData({ wxBusy: true })
        wxUnbind()
          .then((res) => {
            this.setData({ wxBusy: false })
            if (res.code !== 0 || !res.data) {
              wx.showToast({ title: res.message || '解绑失败', icon: 'none' })
              return
            }
            this.setData({ wxBound: !!res.data.wxBound })
            wx.showToast({ title: '已解绑', icon: 'success' })
          })
          .catch((err: Error) => {
            this.setData({ wxBusy: false })
            wx.showToast({ title: (err && err.message) || '解绑失败', icon: 'none' })
          })
      },
    })
  },

  onSwitchTenant() {
    wx.navigateTo({ url: '/pages/select-tenant/select-tenant' })
  },

  onChangePassword() {
    wx.navigateTo({ url: '/pages/mine/change-password' })
  },

  onLogout() {
    if (this.data.loggingOut) {
      return
    }
    wx.showModal({
      title: '确认退出',
      content: '退出后需要重新登录',
      success: (modal) => {
        if (!modal.confirm) {
          return
        }
        this.setData({ loggingOut: true })
        logout()
          .catch(() => {
            // 接口失败仍清本地
          })
          .then(() => {
            clearAllAuthLocal()
            this.setData({ loggingOut: false })
            reLaunchTo('/pages/login/login')
          })
      },
    })
  },
})
