import { getWxLoginCode, login, wxLogin } from '../../services/auth-api'

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    wxLoading: false,
  },

  onUsername(e: WechatMiniprogram.Input) {
    this.setData({ username: (e.detail.value || '').trim() })
  },

  onPassword(e: WechatMiniprogram.Input) {
    this.setData({ password: e.detail.value || '' })
  },

  goSelectTenant() {
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      wx.reLaunch({ url: '/pages/select-tenant/select-tenant' })
    }, 500)
  },

  onSubmit() {
    const username = (this.data.username || '').trim()
    const password = this.data.password || ''
    if (!username) {
      wx.showToast({ title: '请输入登录名', icon: 'none' })
      return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (this.data.loading || this.data.wxLoading) {
      return
    }
    this.setData({ loading: true })
    login(username, password)
      .then((res) => {
        this.setData({ loading: false })
        if (res.code !== 0 || !res.data || !res.data.token) {
          wx.showToast({ title: res.message || '登录失败', icon: 'none' })
          return
        }
        this.goSelectTenant()
      })
      .catch((err: Error) => {
        this.setData({ loading: false })
        wx.showToast({
          title: (err && err.message) || '网络错误',
          icon: 'none',
        })
      })
  },

  onWxLogin() {
    if (this.data.loading || this.data.wxLoading) {
      return
    }
    this.setData({ wxLoading: true })
    getWxLoginCode()
      .then((code) => wxLogin(code))
      .then((res) => {
        this.setData({ wxLoading: false })
        if (res.code !== 0 || !res.data || !res.data.token) {
          wx.showToast({ title: res.message || '微信登录失败', icon: 'none' })
          return
        }
        this.goSelectTenant()
      })
      .catch((err: Error) => {
        this.setData({ wxLoading: false })
        wx.showToast({
          title: (err && err.message) || '微信登录失败',
          icon: 'none',
        })
      })
  },
})
