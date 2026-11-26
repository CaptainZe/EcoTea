import { fetchInfoDetail, InfoSkuItem } from '../../services/info-api'
import { saveImagesToAlbum } from '../../utils/album'
import { assertInfoAccess } from '../../utils/info-guard'
import {
  formatOfficialPlain,
  formatRecyclePlain,
  formatSalePlain,
  recycleExtraLine,
} from '../../utils/info-list-format'
import { buildMarkTags, MarkTag } from '../../utils/mark-tags'

type DetailView = InfoSkuItem & {
  officialPlain: string
  salePlain: string
  saleDiscountShow: string
  recyclePlain: string
  recycleDiscountShow: string
  extraLine: string
  markTags: MarkTag[]
  hasAttrs: boolean
}

function toDetailView(item: InfoSkuItem): DetailView {
  const markTags = buildMarkTags(item)
  return {
    ...item,
    officialPlain: formatOfficialPlain(item),
    salePlain: formatSalePlain(item),
    saleDiscountShow: item.discountShow || '',
    recyclePlain: formatRecyclePlain(item),
    recycleDiscountShow: item.recycleDiscountShow || '',
    extraLine: recycleExtraLine(item),
    markTags,
    hasAttrs: !!(
      markTags.length ||
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
    item: null as InfoSkuItem | null,
    view: null as DetailView | null,
    images: [] as string[],
    imageIndex: 0,
    loading: true,
    saving: false,
    errorTip: '',
  },

  onLoad(query: Record<string, string | undefined>) {
    if (!assertInfoAccess()) {
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

  loadDetail(id: number) {
    this.setData({ loading: true })
    fetchInfoDetail(id)
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

  onCopyTitle() {
    const item = this.data.item
    if (!item) return
    const text = String(item.title || item.name || '').trim()
    if (!text) {
      wx.showToast({ title: '无可复制内容', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '已复制', icon: 'success' }),
    })
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
})
