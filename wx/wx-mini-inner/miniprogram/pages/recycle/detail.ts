import { fetchRecycleDetail, RecycleSkuItem } from '../../services/recycle-api'
import { add as addQuote, has as hasQuote } from '../../services/recycle-quote'
import { saveImagesToAlbum } from '../../utils/album'
import { assertRecycleAccess } from '../../utils/recycle-guard'
import {
  buildWhStockRows,
  formatOfficialPlain,
  formatRecyclePlain,
  formatStockLabel,
  recycleExtraLine,
  stockExtras,
  WhStockRow,
} from '../../utils/recycle-list-format'

type DetailView = RecycleSkuItem & {
  recyclePlain: string
  officialPlain: string
  discountShow: string
  extraLine: string
  stockText: string
  stockScarce: boolean
  stockExtras: string[]
  whRows: WhStockRow[]
  hasAttrs: boolean
}

function toDetailView(item: RecycleSkuItem): DetailView {
  const stock = formatStockLabel(item)
  return {
    ...item,
    recyclePlain: formatRecyclePlain(item),
    officialPlain: formatOfficialPlain(item),
    discountShow: item.recycleDiscountShow || '',
    extraLine: recycleExtraLine(item),
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
    item: null as RecycleSkuItem | null,
    view: null as DetailView | null,
    images: [] as string[],
    imageIndex: 0,
    loading: true,
    saving: false,
    inQuote: false,
    errorTip: '',
  },

  onLoad(query: Record<string, string | undefined>) {
    if (!assertRecycleAccess()) {
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

  loadDetail(id: number) {
    this.setData({ loading: true })
    fetchRecycleDetail(id)
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          this.setData({
            loading: false,
            item: null,
            view: null,
            errorTip: res.message || '暂不可查',
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
    if (!this.data.item) {
      return
    }
    if (this.data.inQuote || hasQuote(this.data.item.id)) {
      wx.showToast({ title: '已在回收报价单中', icon: 'none' })
      return
    }
    const ok = addQuote(this.data.item)
    this.setData({ inQuote: hasQuote(this.data.item.id) })
    wx.showToast({ title: ok ? '已加入报价单' : '加入失败', icon: ok ? 'success' : 'none' })
  },
})
