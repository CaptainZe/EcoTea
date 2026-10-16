import { getCurrentTenant } from '../../services/auth-api'
import {
  fetchOnlineWarehouses,
  WarehouseOption,
} from '../../services/chai-meta-api'
import { fetchSaleList, SaleSkuItem } from '../../services/sale-api'
import { add as addQuote, has as hasQuote, kindCount } from '../../services/sale-quote'
import { SaleListRow, toListRow } from '../../utils/sale-list-format'
import {
  parseYearInput,
  prodBatchFromSheetIndex,
  PROD_BATCH_SHEET_LABELS,
  yearChipLabel,
} from '../../utils/year-filter'
import type { SaleFilterResult } from './filter'

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
    list: [] as SaleListRow[],
    page: 1,
    total: 0,
    matchHint: '',
    loading: false,
    loadingMore: false,
    noMore: false,
    emptyTip: '暂无商品',
    quoteCount: 0,
    /** 原始列表，便于刷新 inQuote 状态 */
    rawList: [] as SaleSkuItem[],

    recycleIndex: 0,
    recycleLabels: ['全部', '新回收'],
    recycleRecent: false,

    warehouses: [] as WarehouseOption[],
    whLabels: ['全部仓'] as string[],
    whIndex: 0,
    whId: 0 as number | 0,

    year: null as number | null,
    yearLabel: '年份',
    prodBatchLabels: PROD_BATCH_SHEET_LABELS.slice(),
    prodBatchIndex: 0,
    prodBatchChipText: '上下年',
    prodBatch: null as number | null,

    brandIds: [] as number[],
    types: [] as number[],
    priceMin: null as number | null,
    priceMax: null as number | null,
    officialPriceMin: null as number | null,
    officialPriceMax: null as number | null,
    nonSale: null as number | null,
    filterActive: false,
    filterSummary: '',
  },

  onLoad() {
    if (!this.assertSalePerm()) {
      return
    }
    this.loadWarehouses()
    this.reload()
  },

  onShow() {
    this.syncQuoteState()
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh())
  },

  mapRows(items: SaleSkuItem[]): SaleListRow[] {
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

  loadWarehouses() {
    fetchOnlineWarehouses()
      .then((res) => {
        const list = res.code === 0 && res.data ? res.data : []
        const labels = ['全部仓'].concat(
          list.map((w) => w.shortName || w.name || String(w.id))
        )
        this.setData({ warehouses: list, whLabels: labels })
      })
      .catch(() => {
        this.setData({ warehouses: [], whLabels: ['全部仓'] })
      })
  },

  computeFilterFlags(
    brandIds: number[],
    types: number[],
    priceMin: number | null,
    priceMax: number | null,
    officialPriceMin: number | null,
    officialPriceMax: number | null,
    nonSale: number | null
  ) {
    const active =
      brandIds.length > 0 ||
      types.length > 0 ||
      priceMin != null ||
      priceMax != null ||
      officialPriceMin != null ||
      officialPriceMax != null ||
      nonSale != null
    const parts: string[] = []
    if (brandIds.length) parts.push('品牌' + brandIds.length)
    if (types.length) parts.push('茶类' + types.length)
    if (priceMin != null || priceMax != null) parts.push('售价')
    if (nonSale === 1) parts.push('非卖品')
    if (nonSale === 0) parts.push('非非卖品')
    if (officialPriceMin != null || officialPriceMax != null) parts.push('官方价')
    return {
      filterActive: active,
      filterSummary: parts.length ? '筛选：' + parts.join(' · ') : '',
    }
  },

  onKeyword(e: WechatMiniprogram.Input) {
    this.setData({ keyword: (e.detail.value || '').trim() })
  },

  onSearch() {
    if (this.data.barcode || this.data.spuId) return
    this.reload()
  },

  onClearMode() {
    this.setData({
      barcode: '',
      spuId: 0,
      keyword: '',
      brandIds: [],
      types: [],
      priceMin: null,
      priceMax: null,
      officialPriceMin: null,
      officialPriceMax: null,
      nonSale: null,
      filterActive: false,
      filterSummary: '',
      recycleIndex: 0,
      recycleRecent: false,
      whIndex: 0,
      whId: 0,
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

  onRecycleChange(e: WechatMiniprogram.PickerChange) {
    const idx = Number(e.detail.value || 0)
    this.setData({
      recycleIndex: idx,
      recycleRecent: idx === 1,
      barcode: '',
    })
    this.reload()
  },

  onWhChange(e: WechatMiniprogram.PickerChange) {
    const idx = Number(e.detail.value || 0)
    const wh = idx > 0 ? this.data.warehouses[idx - 1] : null
    this.setData({
      whIndex: idx,
      whId: wh && wh.id ? wh.id : 0,
      barcode: '',
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
    if (!spuId) {
      return
    }
    this.setData({ spuId, barcode: '', keyword: '' })
    this.reload()
  },

  onOpenFilter() {
    const self = this
    wx.navigateTo({
      url: '/pages/sale/filter',
      events: {
        acceptFilter(data: SaleFilterResult) {
          self.applyFilter(data)
        },
      },
      success(res) {
        res.eventChannel.emit('initFilter', {
          brandIds: self.data.brandIds.slice(),
          types: self.data.types.slice(),
          priceMin: self.data.priceMin,
          priceMax: self.data.priceMax,
          officialPriceMin: self.data.officialPriceMin,
          officialPriceMax: self.data.officialPriceMax,
          nonSale: self.data.nonSale,
        })
      },
    })
  },

  applyFilter(data: SaleFilterResult) {
    const brandIds = (data.brandIds || []).slice()
    const types = (data.types || []).slice()
    const flags = this.computeFilterFlags(
      brandIds,
      types,
      data.priceMin,
      data.priceMax,
      data.officialPriceMin,
      data.officialPriceMax,
      data.nonSale
    )
    this.setData({
      brandIds,
      types,
      priceMin: data.priceMin,
      priceMax: data.priceMax,
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
    return fetchSaleList({
      keyword: barcode || spuId ? '' : this.data.keyword,
      barcode: barcode || undefined,
      spuId: spuId || undefined,
      whId: this.data.whId || undefined,
      recycleRecent: this.data.recycleRecent ? 1 : undefined,
      brandIds: this.data.brandIds,
      types: this.data.types,
      priceMin: this.data.priceMin,
      priceMax: this.data.priceMax,
      officialPriceMin: this.data.officialPriceMin,
      officialPriceMax: this.data.officialPriceMax,
      nonSale: this.data.nonSale,
      year: this.data.year,
      prodBatch: this.data.prodBatch,
      includeWh: 1,
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
        let emptyTip = '暂无在售商品'
        if (barcode) {
          emptyTip = '未找到该条码商品'
        } else if (spuId) {
          emptyTip = '暂无同款现货'
        } else if (
          this.data.keyword ||
          this.data.filterActive ||
          this.data.recycleRecent ||
          this.data.whId ||
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
    if (!id) {
      return
    }
    wx.navigateTo({ url: '/pages/sale/detail?id=' + id })
  },

  onAddQuote(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    if (hasQuote(id)) {
      wx.showToast({ title: '已在报价单中', icon: 'none' })
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
    wx.navigateTo({ url: '/pages/sale/quote' })
  },
})
