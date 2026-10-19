import { fetchRecycleList, RecycleSkuItem } from '../../services/recycle-api'
import {
  addOrInc,
  clear,
  displayTitle,
  formatQuotePlainText,
  getItems,
  getOrderAdd,
  has as hasQuote,
  payableAmount,
  plainAmount,
  remove,
  replace,
  RecycleQuoteItem,
  setOrderAdd,
  setQty,
  setQuotePrice,
  subtotalAmount,
  toNum,
} from '../../services/recycle-quote'
import { saveLocalImageToAlbum } from '../../utils/album'
import { assertRecycleAccess } from '../../utils/recycle-guard'
import { renderRecycleQuoteImageFile } from '../../utils/recycle-quote-image'
import {
  buildTags,
  formatOfficialShort,
  formatRecyclePlain,
  toListRow,
} from '../../utils/recycle-list-format'

type QuoteRow = RecycleQuoteItem & {
  displayTitle: string
  tags: string[]
  refText: string
  discountShort: string
  stockText: string
  stockScarce: boolean
  quotePriceText: string
}

type SwapRow = ReturnType<typeof toListRow> & {
  isCurrent: boolean
}

type ScanPickRow = {
  id: number
  title: string
  tags: string[]
  priceText: string
  stockText: string
  coverImage: string
  inQuote: boolean
  selected: boolean
}

function formatRecycleRef(item: RecycleQuoteItem): string {
  if (item.recyclePrice != null) {
    const n = Number(item.recyclePrice)
    if (!isNaN(n)) return '¥' + String(n)
  }
  if (item.recyclePriceShow) {
    return String(item.recyclePriceShow).replace(/\(.*$/, '').trim()
  }
  return '-'
}

function formatOfficialRef(item: RecycleQuoteItem): string {
  const show = item.officialPriceShow || ''
  if (!show || show === '-') return ''
  if (show === '非卖品') return '官方 非卖品'
  const s = String(show).replace(/元$/, '').trim()
  if (s.indexOf('¥') === 0 || s.indexOf('￥') === 0) return '官方 ' + s
  return '官方 ¥' + s
}

function formatDiscountShort(item: RecycleQuoteItem): string {
  const ds = item.recycleDiscountShow ? String(item.recycleDiscountShow).trim() : ''
  if (ds) return ds.replace(/[（）()]/g, '')
  const q =
    item.quotePrice != null
      ? Number(item.quotePrice)
      : item.recyclePrice != null
        ? Number(item.recyclePrice)
        : NaN
  const o = item.officialPrice != null ? Number(item.officialPrice) : NaN
  if (isNaN(q) || isNaN(o) || o <= 0) return ''
  const d = (q * 10) / o
  return (Math.round(d * 100) / 100).toFixed(2).replace(/\.?0+$/, '') + '折'
}

function formatStockLine(item: RecycleQuoteItem): { text: string; scarce: boolean } {
  let qty = item.totalQty != null ? Number(item.totalQty) : 0
  if (isNaN(qty) || qty < 0) qty = 0
  return {
    scarce: qty <= 3,
    text: '库存 ' + qty + ' 件',
  }
}

function quotePriceValue(item: RecycleQuoteItem): string {
  if (item.quotePrice != null) {
    const n = Number(item.quotePrice)
    if (!isNaN(n)) return String(n)
  }
  if (item.recyclePrice != null) {
    const r = Number(item.recyclePrice)
    if (!isNaN(r)) return String(r)
  }
  return ''
}

function toRow(it: RecycleQuoteItem): QuoteRow {
  const official = formatOfficialRef(it)
  const refParts = ['参考回收价 ' + formatRecycleRef(it)]
  if (official) refParts.push(official)
  const stock = formatStockLine(it)
  return {
    ...it,
    displayTitle: displayTitle(it),
    tags: buildTags(it as RecycleSkuItem),
    refText: refParts.join('  '),
    discountShort: formatDiscountShort(it),
    stockText: stock.text,
    stockScarce: stock.scarce,
    quotePriceText: quotePriceValue(it),
  }
}

Page({
  data: {
    items: [] as QuoteRow[],
    metaText: '',
    subtotalText: '0',
    payableText: '0',
    orderAdd: 0,
    orderAddText: '0',
    orderAddBtnText: '整单改价',
    addOpen: false,
    addDraft: '',
    genLoading: false,
    saveLoading: false,
    previewOpen: false,
    previewPath: '',
    swapOpen: false,
    swapLoading: false,
    swapHint: '',
    swapFromId: 0 as number | 0,
    swapList: [] as SwapRow[],
    swapRaw: [] as RecycleSkuItem[],

    scanOpen: false,
    scanLoading: false,
    scanCode: '',
    scanList: [] as ScanPickRow[],
    scanRaw: [] as RecycleSkuItem[],
  },

  onShow() {
    if (!assertRecycleAccess()) return
    this.refresh()
  },

  noop() {},

  refresh() {
    const items = getItems().map(toRow)
    let pcs = 0
    items.forEach((it) => {
      pcs += it.qty || 1
    })
    const add = getOrderAdd()
    this.setData({
      items,
      metaText: items.length
        ? '共 ' + items.length + ' 种，合计 ' + pcs + ' 件 · 仅本机保存'
        : '',
      subtotalText: plainAmount(subtotalAmount()),
      payableText: plainAmount(payableAmount()),
      orderAdd: add,
      orderAddText: plainAmount(add),
      orderAddBtnText: add > 0 ? '整单改价 +¥' + plainAmount(add) : '整单改价',
    })
  },

  onQtyStep(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id
    const delta = Number(e.currentTarget.dataset.delta || 0)
    const row = this.data.items.find((x) => String(x.id) === String(id))
    if (!row) return
    setQty(id, (row.qty || 1) + delta)
    this.refresh()
  },

  onQtyBlur(e: WechatMiniprogram.Input) {
    setQty(e.currentTarget.dataset.id, e.detail.value)
    this.refresh()
  },

  onPriceBlur(e: WechatMiniprogram.Input) {
    setQuotePrice(e.currentTarget.dataset.id, e.detail.value)
    this.refresh()
  },

  onRemove(e: WechatMiniprogram.TouchEvent) {
    remove(e.currentTarget.dataset.id)
    this.refresh()
  },

  onOpenDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: '/pages/recycle/detail?id=' + id })
  },

  onScan() {
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        const raw = (res && res.result) || ''
        const code = String(raw).replace(/\D/g, '').trim()
        if (!code) {
          wx.showToast({ title: '未识别到条码', icon: 'none' })
          return
        }
        this.openScanResult(code)
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.indexOf('cancel') >= 0) {
          return
        }
        wx.showToast({ title: '扫码失败', icon: 'none' })
      },
    })
  },

  openScanResult(code: string) {
    this.setData({
      scanOpen: true,
      scanLoading: true,
      scanCode: code,
      scanList: [],
      scanRaw: [],
    })
    fetchRecycleList({
      barcode: code,
      page: 1,
      size: 50,
    })
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          this.setData({ scanLoading: false, scanList: [], scanRaw: [] })
          wx.showToast({ title: res.message || '查询失败', icon: 'none' })
          return
        }
        const raw = res.data.list || []
        const list = raw.map((it) => {
          const inQuote = hasQuote(it.id)
          const stock = formatStockLine(it as RecycleQuoteItem)
          return {
            id: it.id,
            title: it.title || it.name || '商品',
            tags: buildTags(it),
            priceText: formatRecyclePlain(it),
            stockText: stock.text,
            coverImage: it.coverImage || '',
            inQuote,
            selected: raw.length === 1 && !inQuote,
          }
        })
        this.setData({
          scanLoading: false,
          scanRaw: raw,
          scanList: list,
        })
      })
      .catch(() => {
        this.setData({ scanLoading: false, scanList: [], scanRaw: [] })
        wx.showToast({ title: '网络异常', icon: 'none' })
      })
  },

  onCloseScan() {
    this.setData({
      scanOpen: false,
      scanLoading: false,
      scanCode: '',
      scanList: [],
      scanRaw: [],
    })
  },

  onToggleScan(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    const hit = (this.data.scanList || []).find((r) => r.id === id)
    if (hit && hit.inQuote) {
      wx.showToast({ title: '已在报价单', icon: 'none' })
      return
    }
    const list = (this.data.scanList || []).map((row) => {
      if (row.id !== id) return row
      return {
        id: row.id,
        title: row.title,
        tags: row.tags,
        priceText: row.priceText,
        stockText: row.stockText,
        coverImage: row.coverImage,
        inQuote: row.inQuote,
        selected: !row.selected,
      }
    })
    this.setData({ scanList: list })
  },

  commitScanSelection(): number {
    const selectedIds: Record<string, boolean> = {}
    ;(this.data.scanList || []).forEach((row) => {
      if (row.selected && !row.inQuote) selectedIds[String(row.id)] = true
    })
    let n = 0
    ;(this.data.scanRaw || []).forEach((it) => {
      if (!selectedIds[String(it.id)]) return
      if (hasQuote(it.id)) return
      const r = addOrInc(it)
      if (r) n += 1
    })
    if (n) this.refresh()
    return n
  },

  onScanContinue() {
    const n = this.commitScanSelection()
    if (n) {
      wx.showToast({ title: '已处理 ' + n + ' 件', icon: 'none' })
    }
    this.setData({
      scanOpen: false,
      scanLoading: false,
      scanCode: '',
      scanList: [],
      scanRaw: [],
    })
    const self = this
    setTimeout(function () {
      self.onScan()
    }, n ? 320 : 0)
  },

  onScanDone() {
    const hasList = (this.data.scanList || []).length > 0
    const hasSelected = (this.data.scanList || []).some((r) => r.selected)
    if (hasList && !hasSelected) {
      wx.showToast({ title: '请先勾选商品', icon: 'none' })
      return
    }
    const n = this.commitScanSelection()
    this.onCloseScan()
    if (n) {
      wx.showToast({ title: '已加入 ' + n + ' 种', icon: 'success' })
    }
  },

  onClear() {
    wx.showModal({
      title: '清空回收报价单',
      content: '确定清空回收报价单？清空后需重新添加商品。',
      confirmText: '清空',
      confirmColor: '#9a3412',
      success: (res) => {
        if (!res.confirm) return
        clear()
        this.refresh()
      },
    })
  },

  onOrderAdd() {
    const add = getOrderAdd()
    this.setData({
      addOpen: true,
      addDraft: add > 0 ? String(add) : '',
    })
  },

  onCloseAdd() {
    this.setData({ addOpen: false })
  },

  onAddInput(e: WechatMiniprogram.Input) {
    this.setData({ addDraft: e.detail.value })
  },

  onClearAdd() {
    setOrderAdd(0)
    this.setData({ addOpen: false, addDraft: '' })
    this.refresh()
    wx.showToast({ title: '已清除加价', icon: 'none' })
  },

  onConfirmAdd() {
    let add = toNum(this.data.addDraft)
    if (add == null || add < 0) add = 0
    setOrderAdd(add)
    this.setData({ addOpen: false })
    this.refresh()
    if (add > 0) {
      wx.showToast({ title: '已设置加价 +¥' + plainAmount(add), icon: 'none' })
    } else {
      wx.showToast({ title: '已清除加价', icon: 'none' })
    }
  },

  onCopyText() {
    const text = formatQuotePlainText()
    if (!text) {
      wx.showToast({ title: '报价单为空', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '已复制', icon: 'success' }),
    })
  },

  async onGenImage() {
    if (!getItems().length) {
      wx.showToast({ title: '报价单为空', icon: 'none' })
      return
    }
    if (this.data.genLoading) return
    this.setData({ genLoading: true })
    wx.showLoading({ title: '正在生成…', mask: true })
    try {
      const path = await renderRecycleQuoteImageFile(
        'quoteCanvas',
        this as unknown as WechatMiniprogram.Page.TrivialInstance
      )
      this.setData({
        previewOpen: true,
        previewPath: path,
      })
    } catch (e) {
      console.warn('[recycle-quote] gen image fail', e)
      wx.showToast({
        title: (e && (e as Error).message) || '生成失败',
        icon: 'none',
      })
    } finally {
      wx.hideLoading()
      this.setData({ genLoading: false })
    }
  },

  onClosePreview() {
    this.setData({ previewOpen: false })
  },

  async onSavePreview() {
    const path = this.data.previewPath
    if (!path) return
    if (this.data.saveLoading) return
    this.setData({ saveLoading: true })
    try {
      await saveLocalImageToAlbum(path)
      wx.showToast({ title: '已保存相册', icon: 'success' })
    } catch (e) {
      wx.showToast({
        title: (e && (e as Error).message) || '保存失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saveLoading: false })
    }
  },

  async onOpenSwap(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id)
    const spuId = Number(e.currentTarget.dataset.spu)
    if (!spuId) {
      wx.showToast({ title: '暂无同款可换', icon: 'none' })
      return
    }
    this.setData({
      swapOpen: true,
      swapLoading: true,
      swapHint: '加载中…',
      swapFromId: id,
      swapList: [],
      swapRaw: [],
    })
    try {
      const res = await fetchRecycleList({
        spuId,
        page: 1,
        size: 50,
      })
      if (res.code !== 0 || !res.data) {
        throw new Error(res.message || '加载失败')
      }
      const list = (res.data.list || []) as RecycleSkuItem[]
      let current: RecycleSkuItem | null = null
      const others: RecycleSkuItem[] = []
      list.forEach((it) => {
        if (!it || it.id == null) return
        if (String(it.id) === String(id)) current = it
        else others.push(it)
      })
      const ordered = current ? [current].concat(others) : others
      const swapList: SwapRow[] = ordered.map((it) => {
        const row = toListRow(it, hasQuote(it.id))
        return {
          ...row,
          isCurrent: String(it.id) === String(id),
          recyclePlain: formatRecyclePlain(it),
          officialShort: formatOfficialShort(it),
        }
      })
      this.setData({
        swapLoading: false,
        swapRaw: ordered,
        swapList,
        swapHint: others.length
          ? '共 ' + ordered.length + ' 款同款 · 点「替换」换入当前行'
          : ordered.length
            ? '仅当前款，暂无其它同款'
            : '',
      })
    } catch (err) {
      console.warn('[recycle-quote] swap fail', err)
      this.setData({
        swapLoading: false,
        swapHint: '',
        swapList: [],
      })
      wx.showToast({
        title: (err && (err as Error).message) || '加载失败',
        icon: 'none',
      })
    }
  },

  onCloseSwap() {
    this.setData({
      swapOpen: false,
      swapList: [],
      swapRaw: [],
      swapFromId: 0,
    })
  },

  onPickSwap(e: WechatMiniprogram.TouchEvent) {
    const newId = e.currentTarget.dataset.id
    const fromId = this.data.swapFromId
    const raw = this.data.swapRaw.find((x) => String(x.id) === String(newId))
    if (!raw || !fromId) return
    if (hasQuote(newId)) {
      wx.showToast({ title: '已在回收报价单中', icon: 'none' })
      return
    }
    const ok = replace(fromId, raw)
    if (!ok) {
      wx.showToast({ title: '替换失败', icon: 'none' })
      return
    }
    this.onCloseSwap()
    this.refresh()
    wx.showToast({ title: '已换同款', icon: 'success' })
  },

  onGoRecycle() {
    wx.navigateTo({ url: '/pages/recycle/list' })
  },
})
