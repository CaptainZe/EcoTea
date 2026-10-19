import { getCurrentTenant } from '../../services/auth-api'
import { COLOR_BRAND, COLOR_RECYCLE } from '../../styles/token-colors'

type EntryDef = {
  key: string
  title: string
  icon: string
  /** 有路由则跳转；否则 toast 即将开放 */
  url?: string
}

type GroupDef = {
  perm: string
  title: string
  accent: string
  entries: EntryDef[]
}

const ALL_GROUPS: GroupDef[] = [
  {
    perm: 'SALE',
    title: '销售',
    accent: COLOR_BRAND,
    entries: [
      { key: 'sale-price', title: '在售价目', icon: '/assets/workbench/sale-price.png', url: '/pages/sale/list' },
      { key: 'sale-quote', title: '报价单', icon: '/assets/workbench/sale-quote.png', url: '/pages/sale/quote' },
      { key: 'sale-order', title: '销售单', icon: '/assets/workbench/sale-order.png' },
    ],
  },
  {
    perm: 'RECYCLE',
    title: '回收',
    accent: COLOR_RECYCLE,
    entries: [
      { key: 'recycle-price', title: '回收价目', icon: '/assets/workbench/recycle-price.png', url: '/pages/recycle/list' },
      { key: 'recycle-quote', title: '报价单', icon: '/assets/workbench/recycle-quote.png', url: '/pages/recycle/quote' },
      { key: 'recycle-order', title: '回收单', icon: '/assets/workbench/recycle-order.png' },
    ],
  },
  // STOCK / FINANCE：暂缓，不展示
  {
    perm: 'SHOP',
    title: '店铺管理',
    accent: '#475569',
    entries: [
      { key: 'shop-employee', title: '员工管理', icon: '/assets/workbench/shop-employee.png' },
    ],
  },
]

Page({
  data: {
    groups: [] as Array<GroupDef & { entries: EntryDef[] }>,
    empty: false,
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    const tenant = getCurrentTenant()
    const permSet = new Set((tenant && tenant.permCodes) || [])
    const groups = ALL_GROUPS.filter((g) => permSet.has(g.perm))
    this.setData({
      groups,
      empty: groups.length === 0,
    })
  },

  onTapEntry(e: WechatMiniprogram.TouchEvent) {
    const url = e.currentTarget.dataset.url as string | undefined
    if (url) {
      wx.navigateTo({ url })
      return
    }
    wx.showToast({ title: '即将开放', icon: 'none' })
  },
})
