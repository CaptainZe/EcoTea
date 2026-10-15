import { changePassword, clearAllAuthLocal } from '../../services/auth-api'
import { reLaunchTo } from '../../services/session'

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    loading: false,
  },

  onOld(e: WechatMiniprogram.Input) {
    this.setData({ oldPassword: e.detail.value || '' })
  },

  onNew(e: WechatMiniprogram.Input) {
    this.setData({ newPassword: e.detail.value || '' })
  },

  onConfirm(e: WechatMiniprogram.Input) {
    this.setData({ confirmPassword: e.detail.value || '' })
  },

  onSubmit() {
    if (this.data.loading) return
    const oldPassword = (this.data.oldPassword || '').trim()
    const newPassword = (this.data.newPassword || '').trim()
    const confirmPassword = (this.data.confirmPassword || '').trim()
    if (!oldPassword) {
      wx.showToast({ title: '请输入原密码', icon: 'none' })
      return
    }
    if (newPassword.length < 6 || newPassword.length > 32) {
      wx.showToast({ title: '新密码须为 6～32 位', icon: 'none' })
      return
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次新密码不一致', icon: 'none' })
      return
    }
    if (newPassword === oldPassword) {
      wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    changePassword(oldPassword, newPassword)
      .then((res) => {
        if (res.code !== 0) {
          wx.showToast({ title: res.message || '修改失败', icon: 'none' })
          this.setData({ loading: false })
          return
        }
        clearAllAuthLocal()
        wx.showToast({ title: '已修改，请重新登录', icon: 'success' })
        setTimeout(() => {
          reLaunchTo('/pages/login/login')
        }, 600)
      })
      .catch((err: Error) => {
        this.setData({ loading: false })
        // 改密成功后服务端已踢会话，后续请求可能 401；仍清本地回登录
        const msg = (err && err.message) || '网络错误'
        if (msg.indexOf('登录已失效') >= 0) {
          clearAllAuthLocal()
          reLaunchTo('/pages/login/login')
          return
        }
        wx.showToast({ title: msg, icon: 'none' })
      })
  },
})
