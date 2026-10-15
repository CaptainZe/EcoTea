import { getCurrentTenant } from '../../services/auth-api'
import { fetchSaleDetail, SaleSkuItem } from '../../services/sale-api'
import {
  buildSaleCopyDraft,
  formatSaleCopyText,
  formatSalePriceWithDiscount,
  officialPriceNum,
} from '../../utils/sale-copy'

/** 数量标题旁库存参考：完整 / 无袋 / 破损 / 破损无袋 */
function stockRefOf(item: SaleSkuItem): string {
  let total = item.totalQty != null ? Number(item.totalQty) : 0
  if (isNaN(total) || total < 0) total = 0
  const noBag = item.qtyNoBag != null ? Number(item.qtyNoBag) : 0
  const damaged = item.qtyDamaged != null ? Number(item.qtyDamaged) : 0
  const damagedNoBag = item.qtyDamagedNoBag != null ? Number(item.qtyDamagedNoBag) : 0
  const extras =
    (isNaN(noBag) ? 0 : Math.max(0, noBag)) +
    (isNaN(damaged) ? 0 : Math.max(0, damaged)) +
    (isNaN(damagedNoBag) ? 0 : Math.max(0, damagedNoBag))
  const intact = Math.max(0, Math.floor(total) - extras)
  const parts: string[] = ['完整 ' + intact]
  if (!isNaN(noBag) && noBag > 0) parts.push('无袋 ' + noBag)
  if (!isNaN(damaged) && damaged > 0) parts.push('破损 ' + damaged)
  if (!isNaN(damagedNoBag) && damagedNoBag > 0) parts.push('破损无袋 ' + damagedNoBag)
  return parts.join(' · ')
}

Page({
  data: {
    id: 0,
    item: null as SaleSkuItem | null,
    loading: true,
    ready: false,
    errorTip: '',
    packLine: '',
    salePriceNum: '',
    qty: '',
    remark: '',
    preview: '',
    discountHint: '',
    stockRef: '',
  },

  onLoad(query: Record<string, string | undefined>) {
    if (!this.assertSalePerm()) return
    const id = Number(query.id || 0)
    if (!id) {
      this.setData({ loading: false, errorTip: '参数无效' })
      return
    }
    this.setData({ id })
    this.load(id)
  },

  assertSalePerm(): boolean {
    const tenant = getCurrentTenant()
    const perms = (tenant && tenant.permCodes) || []
    if (!tenant || !tenant.tenantId) {
      wx.showToast({ title: '请先选择店铺', icon: 'none' })
      setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
      return false
    }
    if (perms.indexOf('SALE') < 0) {
      wx.showToast({ title: '无销售权限', icon: 'none' })
      setTimeout(() => wx.navigateBack({ fail: () => {} }), 400)
      return false
    }
    return true
  },

  load(id: number) {
    this.setData({ loading: true })
    fetchSaleDetail(id)
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          this.setData({
            loading: false,
            ready: false,
            errorTip: res.message || '暂不可售',
          })
          return
        }
        const item = res.data
        const draft = buildSaleCopyDraft(item)
        this.setData(
          {
            item,
            loading: false,
            ready: true,
            packLine: draft.packLine,
            salePriceNum: draft.salePriceNum,
            qty: draft.qty,
            remark: draft.remark,
            stockRef: stockRefOf(item),
            errorTip: '',
          },
          () => this.refreshPreview()
        )
      })
      .catch((err: Error) => {
        this.setData({
          loading: false,
          ready: false,
          errorTip: (err && err.message) || '加载失败',
        })
      })
  },

  refreshPreview() {
    const item = this.data.item
    if (!item) return
    const draft = {
      packLine: this.data.packLine,
      salePriceNum: this.data.salePriceNum,
      qty: this.data.qty,
      remark: this.data.remark,
    }
    const saleLine = formatSalePriceWithDiscount(
      draft.salePriceNum,
      officialPriceNum(item)
    )
    let discountHint = ''
    const m = saleLine.match(/（([^）]+)）/)
    if (m) discountHint = m[1]
    this.setData({
      preview: formatSaleCopyText(item, draft),
      discountHint,
    })
  },

  onPack(e: WechatMiniprogram.Input) {
    this.setData({ packLine: e.detail.value }, () => this.refreshPreview())
  },

  onSale(e: WechatMiniprogram.Input) {
    this.setData({ salePriceNum: e.detail.value }, () => this.refreshPreview())
  },

  onQty(e: WechatMiniprogram.Input) {
    this.setData({ qty: e.detail.value }, () => this.refreshPreview())
  },

  onRemark(e: WechatMiniprogram.Input) {
    this.setData({ remark: e.detail.value }, () => this.refreshPreview())
  },

  onCopy() {
    const text = this.data.preview
    if (!text) {
      wx.showToast({ title: '文案为空', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: text,
      success: () => {
        // 系统已有「内容已复制」提示，稍后再补一次避免被盖住
        setTimeout(() => {
          wx.showToast({ title: '已复制', icon: 'success' })
        }, 300)
      },
      fail: (err) => {
        wx.showToast({
          title: (err && err.errMsg) || '复制失败',
          icon: 'none',
        })
      },
    })
  },
})
