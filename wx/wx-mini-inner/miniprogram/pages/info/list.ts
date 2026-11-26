import { fetchInfoList, InfoSkuItem } from '../../services/info-api'
import { assertInfoAccess } from '../../utils/info-guard'
import { InfoListRow, toListRow } from '../../utils/info-list-format'
import {
  parseYearInput,
  prodBatchFromSheetIndex,
  PROD_BATCH_SHEET_LABELS,
  yearChipLabel,
} from '../../utils/year-filter'
import type { InfoFilterResult } from './filter'

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
    list: [] as InfoListRow[],
    page: 1,
    total: 0,
    matchHint: '',
    loading: false,
    loadingMore: false,
    noMore: false,
    emptyTip: '暂无商品',
    rawList: [] as InfoSkuItem[],

    brandIds: [] as number[],
    brandNames: [] as string[],
    types: [] as number[],
    priceMin: null as number | null,
    priceMax: null as number | null,
    recyclePriceMin: null as number | null,
    recyclePriceMax: null as number | null,
    officialPriceMin: null as number | null,
    officialPriceMax: null as number | null,
    nonSale: null as number | null,
    hideExternal: null as number | null,
    coreFlag: null as number | null,
    tastingFlag: null as number | null,
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
    if (!assertInfoAccess()) {
      return
    }
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.loading || this.data.loadingMore || this.data.noMore) {
      return
    }
    this.loadMore()
  },

  mapRows(items: InfoSkuItem[]): InfoListRow[] {
    return items.map((it) => toListRow(it))
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
      priceMin: null,
      priceMax: null,
      recyclePriceMin: null,
      recyclePriceMax: null,
      officialPriceMin: null,
      officialPriceMax: null,
      nonSale: null,
      hideExternal: null,
      coreFlag: null,
      tastingFlag: null,
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
    priceMin: number | null,
    priceMax: number | null,
    recyclePriceMin: number | null,
    recyclePriceMax: number | null,
    officialPriceMin: number | null,
    officialPriceMax: number | null,
    nonSale: number | null,
    hideExternal: number | null,
    coreFlag: number | null,
    tastingFlag: number | null
  ) {
    const active =
      brandIds.length > 0 ||
      types.length > 0 ||
      priceMin != null ||
      priceMax != null ||
      recyclePriceMin != null ||
      recyclePriceMax != null ||
      officialPriceMin != null ||
      officialPriceMax != null ||
      nonSale != null ||
      hideExternal != null ||
      coreFlag != null ||
      tastingFlag != null
    const parts: string[] = []
    if (brandIds.length) parts.push('品牌' + brandIds.length)
    if (types.length) parts.push('茶类' + types.length)
    if (priceMin != null || priceMax != null) parts.push('售价')
    if (recyclePriceMin != null || recyclePriceMax != null) parts.push('回收价')
    if (nonSale === 1) parts.push('非卖品')
    if (nonSale === 0) parts.push('非非卖品')
    if (officialPriceMin != null || officialPriceMax != null) parts.push('官方价')
    if (hideExternal === 1) parts.push('对外隐藏')
    if (hideExternal === 0) parts.push('非对外隐藏')
    if (coreFlag === 1) parts.push('核心')
    if (coreFlag === 0) parts.push('非核心')
    if (tastingFlag === 1) parts.push('品鉴')
    if (tastingFlag === 0) parts.push('非品鉴')
    return {
      filterActive: active,
      filterSummary: parts.length ? '筛选：' + parts.join(' · ') : '',
    }
  },

  onOpenFilter() {
    const self = this
    wx.navigateTo({
      url: '/pages/info/filter',
      events: {
        acceptFilter(data: InfoFilterResult) {
          self.applyFilter(data)
        },
      },
      success(res) {
        res.eventChannel.emit('initFilter', {
          brandIds: self.data.brandIds.slice(),
          brandNames: self.data.brandNames.slice(),
          types: self.data.types.slice(),
          priceMin: self.data.priceMin,
          priceMax: self.data.priceMax,
          recyclePriceMin: self.data.recyclePriceMin,
          recyclePriceMax: self.data.recyclePriceMax,
          officialPriceMin: self.data.officialPriceMin,
          officialPriceMax: self.data.officialPriceMax,
          nonSale: self.data.nonSale,
          hideExternal: self.data.hideExternal,
          coreFlag: self.data.coreFlag,
          tastingFlag: self.data.tastingFlag,
        })
      },
    })
  },

  applyFilter(data: InfoFilterResult) {
    const brandIds = (data.brandIds || []).slice()
    const brandNames = (data.brandNames || []).slice()
    const types = (data.types || []).slice()
    const flags = this.computeFilterFlags(
      brandIds,
      types,
      data.priceMin,
      data.priceMax,
      data.recyclePriceMin,
      data.recyclePriceMax,
      data.officialPriceMin,
      data.officialPriceMax,
      data.nonSale,
      data.hideExternal,
      data.coreFlag,
      data.tastingFlag
    )
    this.setData({
      brandIds,
      brandNames,
      types,
      priceMin: data.priceMin,
      priceMax: data.priceMax,
      recyclePriceMin: data.recyclePriceMin,
      recyclePriceMax: data.recyclePriceMax,
      officialPriceMin: data.officialPriceMin,
      officialPriceMax: data.officialPriceMax,
      nonSale: data.nonSale,
      hideExternal: data.hideExternal,
      coreFlag: data.coreFlag,
      tastingFlag: data.tastingFlag,
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
    return fetchInfoList({
      keyword: barcode || spuId ? '' : this.data.keyword,
      barcode: barcode || undefined,
      spuId: spuId || undefined,
      brandIds: this.data.brandIds,
      types: this.data.types,
      priceMin: this.data.priceMin,
      priceMax: this.data.priceMax,
      recyclePriceMin: this.data.recyclePriceMin,
      recyclePriceMax: this.data.recyclePriceMax,
      officialPriceMin: this.data.officialPriceMin,
      officialPriceMax: this.data.officialPriceMax,
      nonSale: this.data.nonSale,
      year: this.data.year,
      prodBatch: this.data.prodBatch,
      hideExternal: this.data.hideExternal,
      coreFlag: this.data.coreFlag,
      tastingFlag: this.data.tastingFlag,
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
        let emptyTip = '暂无商品'
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
    wx.navigateTo({ url: '/pages/info/detail?id=' + id })
  },
})
