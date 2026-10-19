import { fetchRecycleList, RecycleSkuItem } from '../../services/recycle-api'
import { add as addQuote, has as hasQuote, kindCount } from '../../services/recycle-quote'
import { assertRecycleAccess } from '../../utils/recycle-guard'
import { RecycleListRow, toListRow } from '../../utils/recycle-list-format'
import {
  parseYearInput,
  prodBatchFromSheetIndex,
  PROD_BATCH_SHEET_LABELS,
  yearChipLabel,
} from '../../utils/year-filter'
import type { RecycleFilterResult } from './filter'

const PAGE_SIZE = 20

function matchHintOf(matchType?: string): string {
  if (matchType === 'brand_exact') return '品牌精确'
  if (matchType === 'name_like') return '品名模糊'
  if (matchType === 'spu') return '同款'
  if (matchType === 'barcode') return '条码精确'
  return ''
}

Page({
  data: {
    keyword: '',
    barcode: '',
    spuId: 0 as number | 0,
    list: [] as RecycleListRow[],
    page: 1,
    total: 0,
    matchHint: '',
    loading: false,
    loadingMore: false,
    noMore: false,
    emptyTip: '暂无回收商品',
    quoteCount: 0,
    rawList: [] as RecycleSkuItem[],

    brandIds: [] as number[],
    brandNames: [] as string[],
    types: [] as number[],
    recyclePriceMin: null as number | null,
    recyclePriceMax: null as number | null,
    officialPriceMin: null as number | null,
    officialPriceMax: null as number | null,
    nonSale: null as number | null,
    filterActive: false,
    filterSummary: '',

    year: null as number | null,
    yearLabel: '年份',
    prodBatchLabels: PROD_BATCH_SHEET_LABELS.slice(),
    prodBatchIndex: 0,
    prodBatchChipText: '上下年',
    prodBatch: null as number | null,
  },

  onLoad() {
    if (!assertRecycleAccess()) {
      return
    }
    this.reload()
  },

  onShow() {
    this.syncQuoteState()
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh())
  },

  mapRows(items: RecycleSkuItem[]): RecycleListRow[] {
    return items.map((it) => toListRow(it, hasQuote(it.id)))
  },

  syncQuoteState() {
    const raw = this.data.rawList || []
    this.setData({
      quoteCount: kindCount(),
      list: this.mapRows(raw),
    })
  },

  onReachBottom() {
    if (this.data.loading || this.data.loadingMore || this.data.noMore) {
      return
    }
    this.loadMore()
  },

  onKeyword(e: WechatMiniprogram.Input) {
    this.setData({ keyword: (e.detail.value || '').trim() })
  },

  onSearch() {
    if (this.data.barcode) return
    if (this.data.spuId) {
      this.setData({ spuId: 0 }, () => this.reload())
      return
    }
    this.reload()
  },

  onClearSpu() {
    if (!this.data.spuId) return
    this.setData({ spuId: 0 }, () => this.reload())
  },

  onClearMode() {
    this.setData({
      barcode: '',
      spuId: 0,
      keyword: '',
      brandIds: [],
      brandNames: [],
      types: [],
      recyclePriceMin: null,
      recyclePriceMax: null,
      officialPriceMin: null,
      officialPriceMax: null,
      nonSale: null,
      filterActive: false,
      filterSummary: '',
      year: null,
      yearLabel: '年份',
      prodBatchIndex: 0,
      prodBatchChipText: '上下年',
      prodBatch: null,
    })
    this.reload()
  },

  onYearTap() {
    const self = this
    const current = this.data.year != null ? String(this.data.year) : ''
    wx.showModal({
      title: '按年份查询',
      editable: true,
      placeholderText: '输入4位年份，留空清除',
      content: current,
      success(res) {
        if (!res.confirm) {
          return
        }
        const parsed = parseYearInput(res.content || '')
        if (!parsed.ok) {
          wx.showToast({ title: parsed.message, icon: 'none' })
          return
        }
        self.setData({
          year: parsed.year,
          yearLabel: yearChipLabel(parsed.year),
        })
        self.reload()
      },
    })
  },

  onProdBatchChange(e: WechatMiniprogram.PickerChange) {
    const idx = Number(e.detail.value || 0)
    const labels = this.data.prodBatchLabels || []
    this.setData({
      prodBatchIndex: idx,
      prodBatch: prodBatchFromSheetIndex(idx),
      prodBatchChipText: idx > 0 ? labels[idx] || '上下年' : '上下年',
    })
    this.reload()
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
        this.setData({ barcode: code, keyword: '', spuId: 0 })
        this.reload()
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.indexOf('cancel') >= 0) {
          return
        }
        wx.showToast({ title: '扫码失败', icon: 'none' })
      },
    })
  },

  onSameSpu(e: WechatMiniprogram.TouchEvent) {
    const spuId = Number(e.currentTarget.dataset.spu || 0)
    if (!spuId) return
    this.setData({ spuId, barcode: '', keyword: '' })
    this.reload()
  },

  computeFilterFlags(
    brandIds: number[],
    types: number[],
    recyclePriceMin: number | null,
    recyclePriceMax: number | null,
    officialPriceMin: number | null,
    officialPriceMax: number | null,
    nonSale: number | null
  ) {
    const active =
      brandIds.length > 0 ||
      types.length > 0 ||
      recyclePriceMin != null ||
      recyclePriceMax != null ||
      officialPriceMin != null ||
      officialPriceMax != null ||
      nonSale != null
    const parts: string[] = []
    if (brandIds.length) parts.push('品牌' + brandIds.length)
    if (types.length) parts.push('茶类' + types.length)
    if (recyclePriceMin != null || recyclePriceMax != null) parts.push('回收价')
    if (nonSale === 1) parts.push('非卖品')
    if (nonSale === 0) parts.push('非非卖品')
    if (officialPriceMin != null || officialPriceMax != null) parts.push('官方价')
    return {
      filterActive: active,
      filterSummary: parts.length ? '筛选：' + parts.join(' · ') : '',
    }
  },

  onOpenFilter() {
    const self = this
    wx.navigateTo({
      url: '/pages/recycle/filter',
      events: {
        acceptFilter(data: RecycleFilterResult) {
          self.applyFilter(data)
        },
      },
      success(res) {
        res.eventChannel.emit('initFilter', {
          brandIds: self.data.brandIds.slice(),
          brandNames: self.data.brandNames.slice(),
          types: self.data.types.slice(),
          recyclePriceMin: self.data.recyclePriceMin,
          recyclePriceMax: self.data.recyclePriceMax,
          officialPriceMin: self.data.officialPriceMin,
          officialPriceMax: self.data.officialPriceMax,
          nonSale: self.data.nonSale,
        })
      },
    })
  },

  applyFilter(data: RecycleFilterResult) {
    const brandIds = (data.brandIds || []).slice()
    const brandNames = (data.brandNames || []).slice()
    const types = (data.types || []).slice()
    const flags = this.computeFilterFlags(
      brandIds,
      types,
      data.recyclePriceMin,
      data.recyclePriceMax,
      data.officialPriceMin,
      data.officialPriceMax,
      data.nonSale
    )
    this.setData({
      brandIds,
      brandNames,
      types,
      recyclePriceMin: data.recyclePriceMin,
      recyclePriceMax: data.recyclePriceMax,
      officialPriceMin: data.officialPriceMin,
      officialPriceMax: data.officialPriceMax,
      nonSale: data.nonSale,
      filterActive: flags.filterActive,
      filterSummary: flags.filterSummary,
      barcode: '',
    })
    this.reload()
  },

  reload(): Promise<void> {
    this.setData({ page: 1, noMore: false, list: [], rawList: [], loading: true, matchHint: '' })
    return this.fetchPage(1, true)
  },

  loadMore() {
    const next = this.data.page + 1
    this.setData({ loadingMore: true })
    this.fetchPage(next, false)
  },

  fetchPage(page: number, replace: boolean): Promise<void> {
    const barcode = this.data.barcode || ''
    const spuId = this.data.spuId || 0
    return fetchRecycleList({
      keyword: barcode || spuId ? '' : this.data.keyword,
      barcode: barcode || undefined,
      spuId: spuId || undefined,
      brandIds: this.data.brandIds,
      types: this.data.types,
      recyclePriceMin: this.data.recyclePriceMin,
      recyclePriceMax: this.data.recyclePriceMax,
      officialPriceMin: this.data.officialPriceMin,
      officialPriceMax: this.data.officialPriceMax,
      nonSale: this.data.nonSale,
      year: this.data.year,
      prodBatch: this.data.prodBatch,
      page,
      size: PAGE_SIZE,
    })
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          wx.showToast({ title: res.message || '加载失败', icon: 'none' })
          this.setData({ loading: false, loadingMore: false, emptyTip: '加载失败' })
          return
        }
        const chunkRaw = res.data.list || []
        const rawList = replace ? chunkRaw : (this.data.rawList || []).concat(chunkRaw)
        const list = this.mapRows(rawList)
        const total = res.data.total || 0
        let emptyTip = '暂无回收商品'
        if (barcode) {
          emptyTip = '未找到该条码商品'
        } else if (spuId) {
          emptyTip = '暂无同款'
        } else if (
          this.data.keyword ||
          this.data.filterActive ||
          this.data.year ||
          this.data.prodBatch
        ) {
          emptyTip = '未找到相关商品'
        }
        this.setData({
          rawList,
          list,
          page,
          total,
          matchHint: matchHintOf(res.data.matchType),
          loading: false,
          loadingMore: false,
          noMore: list.length >= total || chunkRaw.length < PAGE_SIZE,
          emptyTip,
          quoteCount: kindCount(),
        })
      })
      .catch((err: Error) => {
        this.setData({ loading: false, loadingMore: false, emptyTip: '加载失败' })
        wx.showToast({ title: (err && err.message) || '网络错误', icon: 'none' })
      })
  },

  onTapItem(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: '/pages/recycle/detail?id=' + id })
  },

  onAddQuote(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    if (hasQuote(id)) {
      wx.showToast({ title: '已在回收报价单中', icon: 'none' })
      return
    }
    const raw = (this.data.rawList || []).find((x) => Number(x.id) === id)
    if (!raw) {
      wx.showToast({ title: '商品不存在', icon: 'none' })
      return
    }
    const ok = addQuote(raw)
    this.syncQuoteState()
    wx.showToast({ title: ok ? '已加入报价单' : '加入失败', icon: ok ? 'success' : 'none' })
  },

  onOpenQuote() {
    wx.navigateTo({ url: '/pages/recycle/quote' })
  },
})
