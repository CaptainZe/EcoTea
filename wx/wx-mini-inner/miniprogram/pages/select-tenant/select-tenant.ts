import { fetchTenants, getCachedTenants, selectTenant, TenantOption } from '../../services/auth-api'
import { getToken } from '../../services/token'

Page({
  data: {
    loading: true,
    submitting: false,
    tenants: [] as TenantOption[],
    selectedId: 0 as number,
    selectedName: '',
  },

  onShow() {
    if (!getToken()) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' })
      }, 400)
      return
    }
    this.loadTenants()
  },

  loadTenants() {
    this.setData({ loading: true })
    const cached = getCachedTenants()
    if (cached.length) {
      this.setData({ tenants: cached, loading: false })
      if (cached.length === 1) {
        this.setData({
          selectedId: cached[0].tenantId,
          selectedName: cached[0].name,
        })
      }
    }
    fetchTenants()
      .then((res) => {
        if (res.code !== 0) {
          wx.showToast({ title: res.message || '加载店铺失败', icon: 'none' })
          this.setData({ loading: false })
          if (!cached.length) {
            this.setData({ tenants: [] })
          }
          return
        }
        const list = res.data || []
        const patch: WechatMiniprogram.IAnyObject = { tenants: list, loading: false }
        if (list.length === 1) {
          patch.selectedId = list[0].tenantId
          patch.selectedName = list[0].name
        }
        this.setData(patch)
      })
      .catch((err: Error) => {
        console.error('[E1.4] tenants fail', err)
        this.setData({ loading: false })
        if (!cached.length) {
          this.setData({ tenants: [] })
        }
        wx.showToast({ title: (err && err.message) || '网络错误', icon: 'none' })
      })
  },

  onPick(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id)
    const name = String(e.currentTarget.dataset.name || '')
    this.setData({ selectedId: id, selectedName: name })
  },

  onConfirm() {
    const tenantId = this.data.selectedId
    if (!tenantId) {
      wx.showToast({ title: '请选择店铺', icon: 'none' })
      return
    }
    if (this.data.submitting) {
      return
    }
    this.setData({ submitting: true })
    selectTenant(tenantId)
      .then((res) => {
        this.setData({ submitting: false })
        if (res.code !== 0 || !res.data || !res.data.tenantSelected) {
          wx.showToast({ title: res.message || '选店失败', icon: 'none' })
          return
        }
        console.log('[E1.4] select-tenant ok', res.data)
        wx.showToast({ title: '已进入 ' + (res.data.tenantName || ''), icon: 'success' })
        setTimeout(() => {
          // tab 页须用 reLaunch / switchTab
          wx.switchTab({ url: '/pages/index/index' })
        }, 500)
      })
      .catch((err: Error) => {
        this.setData({ submitting: false })
        console.error('[E1.4] select-tenant fail', err)
        wx.showToast({ title: (err && err.message) || '网络错误', icon: 'none' })
      })
  },
})
