import { getCurrentTenant } from '../../services/auth-api'

Page({
  data: {
    tenantName: '',
    showSale: false,
    showRecycle: false,
    showInfo: false,
  },

  onShow() {
    const tenant = getCurrentTenant()
    const perms = (tenant && tenant.permCodes) || []
    this.setData({
      tenantName: (tenant && tenant.tenantName) || '',
      showSale: perms.indexOf('SALE') >= 0,
      showRecycle: perms.indexOf('RECYCLE') >= 0,
      showInfo: perms.indexOf('INFO') >= 0,
    })
  },

  onSalePrice() {
    wx.navigateTo({ url: '/pages/sale/list' })
  },

  onRecyclePrice() {
    wx.navigateTo({ url: '/pages/recycle/list' })
  },

  onTeaInfo() {
    wx.navigateTo({ url: '/pages/info/list' })
  },
})
