import { getCurrentTenant } from '../../services/auth-api'
import { fetchSaleDetail, SaleSkuItem } from '../../services/sale-api'
import { add as addQuote, has as hasQuote } from '../../services/sale-quote'
import { saveImagesToAlbum } from '../../utils/album'
import {
  buildWhStockRows,
  formatOfficialPlain,
  formatSalePlain,
  formatStockLabel,
  stockExtras,
  WhStockRow,
} from '../../utils/sale-list-format'

type DetailView = SaleSkuItem & {
  salePlain: string
  officialPlain: string
  discountShow: string
  stockText: string
  stockScarce: boolean
  stockExtras: string[]
  whRows: WhStockRow[]
  hasAttrs: boolean
}

function toDetailView(item: SaleSkuItem): DetailView {
  const stock = formatStockLabel(item)
  return {
    ...item,
    salePlain: formatSalePlain(item),
    officialPlain: formatOfficialPlain(item),
    discountShow: item.discountShow || '',
    stockText: stock.text,
    stockScarce: stock.scarce,
    stockExtras: stockExtras(item),
    whRows: buildWhStockRows(item.warehouseStocks),
    hasAttrs: !!(
      item.gradeName ||
      item.specShow ||
      item.prodBatchShow ||
      item.expirationName ||
      item.skuCode
    ),
  }
}

Page({
  data: {
    id: 0,
    item: null as SaleSkuItem | null,
    view: null as DetailView | null,
    images: [] as string[],
    imageIndex: 0,
    loading: true,
    saving: false,
    inQuote: false,
    errorTip: '',
  },

  onLoad(query: Record<string, string | undefined>) {
    if (!this.assertSalePerm()) {
      return
    }
    const id = Number(query.id || 0)
    if (!id) {
      this.setData({ loading: false, errorTip: '参数无效' })
      return
    }
    this.setData({ id })
    this.loadDetail(id)
  },

  onShow() {
    if (this.data.id) {
      this.setData({ inQuote: hasQuote(this.data.id) })
    }
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

  loadDetail(id: number) {
    this.setData({ loading: true })
    fetchSaleDetail(id)
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          this.setData({
            loading: false,
            item: null,
            view: null,
            errorTip: res.message || '暂不可售',
          })
          return
        }
        const item = res.data
        const images = (item.imageUrls && item.imageUrls.length
          ? item.imageUrls
          : item.coverImage
            ? [item.coverImage]
            : []) as string[]
        this.setData({
          item,
          view: toDetailView(item),
          images,
          imageIndex: 0,
          loading: false,
          inQuote: hasQuote(item.id),
          errorTip: '',
        })
      })
      .catch((err: Error) => {
        this.setData({
          loading: false,
          item: null,
          view: null,
          errorTip: (err && err.message) || '加载失败',
        })
      })
  },

  onSwiperChange(e: WechatMiniprogram.SwiperChange) {
    this.setData({ imageIndex: Number(e.detail.current || 0) })
  },

  onPreview() {
    const urls = this.data.images
    if (!urls.length) {
      return
    }
    const current = urls[this.data.imageIndex] || urls[0]
    wx.previewImage({ urls, current })
  },

  onCopySkuCode() {
    const code = this.data.item && this.data.item.skuCode
    if (!code) {
      return
    }
    wx.setClipboardData({
      data: String(code),
      success: () => wx.showToast({ title: '编码已复制', icon: 'success' }),
    })
  },

  onOpenCopy() {
    const id = this.data.id
    if (!id) return
    wx.navigateTo({ url: '/pages/sale/copy?id=' + id })
  },

  onSaveImages() {
    if (this.data.saving || !this.data.images.length) {
      return
    }
    this.setData({ saving: true })
    saveImagesToAlbum(this.data.images)
      .then(({ ok, fail }) => {
        this.setData({ saving: false })
        if (ok === 0 && fail === 0) {
          wx.showToast({ title: '暂无图片', icon: 'none' })
          return
        }
        if (fail === 0) {
          wx.showToast({ title: '已保存 ' + ok + ' 张', icon: 'success' })
          return
        }
        wx.showToast({
          title: '成功 ' + ok + '，失败 ' + fail,
          icon: 'none',
        })
      })
      .catch((err: Error) => {
        this.setData({ saving: false })
        wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
      })
  },

  onAddQuote() {
    if (this.data.inQuote || !this.data.item) {
      wx.showToast({ title: '已在报价单中', icon: 'none' })
      return
    }
    const ok = addQuote(this.data.item)
    this.setData({ inQuote: hasQuote(this.data.item.id) })
    wx.showToast({ title: ok ? '已加入报价单' : '加入失败', icon: ok ? 'success' : 'none' })
  },
})
