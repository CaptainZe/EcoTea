import {
  BrandPickerItem,
  DictOption,
  fetchBrandPicker,
  fetchChaiTypes,
} from '../../services/chai-meta-api'

/** 筛选页回传给列表 */
export type SaleFilterResult = {
  brandIds: number[]
  types: number[]
  priceMin: number | null
  priceMax: number | null
  officialPriceMin: number | null
  officialPriceMax: number | null
  nonSale: number | null
}

type TagType = DictOption & { on: boolean }
type CommonBrandTag = BrandPickerItem & { on: boolean }

function parsePrice(raw: string): number | null {
  const s = (raw || '').trim()
  if (!s) return null
  const n = Number(s)
  return isNaN(n) ? null : n
}

function normalizePair(min: number | null, max: number | null): { min: number | null; max: number | null } {
  if (min != null && max != null && min > max) {
    return { min: max, max: min }
  }
  return { min, max }
}

function brandSummaryOf(ids: number[], names: string[]): string {
  if (!ids.length) return '全部品牌'
  if (names.length) {
    const shown = names.slice(0, 3).join('、')
    return ids.length > 3 ? shown + ' 等' + ids.length + '个' : shown
  }
  return '已选 ' + ids.length + ' 个 · 全部品牌'
}

Page({
  data: {
    loadingMeta: true,
    loadingBrand: true,
    typesAll: [] as DictOption[],
    typeTags: [] as TagType[],
    commonRaw: [] as BrandPickerItem[],
    commonTags: [] as CommonBrandTag[],

    brandIds: [] as number[],
    brandNames: [] as string[],
    brandSummary: '全部品牌',

    types: [] as number[],
    priceMin: '',
    priceMax: '',
    officialMin: '',
    officialMax: '',
    nonSale: null as number | null,
  },

  _channel: null as WechatMiniprogram.EventChannel | null,
  _nameMap: {} as Record<number, string>,

  onLoad() {
    const channel = this.getOpenerEventChannel()
    this._channel = channel
    channel.on('initFilter', (payload: SaleFilterResult & { brandNames?: string[] }) => {
      const brandIds = (payload && payload.brandIds) || []
      const brandNames = (payload && payload.brandNames) || []
      const types = (payload && payload.types) || []
      const nonSale = payload ? payload.nonSale : null
      brandIds.forEach((id, i) => {
        if (brandNames[i]) this._nameMap[id] = brandNames[i]
      })
      this.setData({
        brandIds: brandIds.slice(),
        brandNames: brandNames.slice(),
        brandSummary: brandSummaryOf(brandIds, brandNames),
        types: types.slice(),
        priceMin: payload && payload.priceMin != null ? String(payload.priceMin) : '',
        priceMax: payload && payload.priceMax != null ? String(payload.priceMax) : '',
        officialMin:
          payload && payload.officialPriceMin != null ? String(payload.officialPriceMin) : '',
        officialMax:
          payload && payload.officialPriceMax != null ? String(payload.officialPriceMax) : '',
        nonSale: nonSale == null ? null : nonSale,
      })
      this.refreshTypeTags(types)
      this.refreshCommonTags(brandIds)
    })
    this.loadTypes()
    this.loadCommonBrands()
  },

  loadTypes() {
    fetchChaiTypes()
      .then((res) => {
        const list = res.code === 0 && res.data ? res.data : []
        this.setData({ typesAll: list, loadingMeta: false })
        this.refreshTypeTags(this.data.types)
      })
      .catch(() => {
        this.setData({ typesAll: [], loadingMeta: false })
        this.refreshTypeTags(this.data.types)
      })
  },

  loadCommonBrands() {
    fetchBrandPicker()
      .then((res) => {
        const common = res.code === 0 && res.data ? res.data.common || [] : []
        common.forEach((b) => {
          if (b.id != null) this._nameMap[b.id] = b.name
        })
        this.setData({ commonRaw: common, loadingBrand: false }, () => {
          this.refreshCommonTags(this.data.brandIds)
        })
      })
      .catch(() => {
        this.setData({ commonRaw: [], loadingBrand: false }, () => {
          this.refreshCommonTags(this.data.brandIds)
        })
      })
  },

  refreshTypeTags(types: number[]) {
    const typeSet = new Set(types.map(String))
    this.setData({
      typeTags: this.data.typesAll.map((t) => ({
        ...t,
        on: typeSet.has(String(t.code)),
      })),
    })
  },

  refreshCommonTags(brandIds: number[]) {
    const idSet = new Set(brandIds)
    this.setData({
      commonTags: (this.data.commonRaw || []).map((b) => ({
        ...b,
        on: idSet.has(b.id),
      })),
    })
  },

  syncBrandState(brandIds: number[]) {
    const brandNames = brandIds.map((id) => this._nameMap[id] || '').filter(Boolean)
    this.setData({
      brandIds,
      brandNames,
      brandSummary: brandSummaryOf(brandIds, brandNames),
    })
    this.refreshCommonTags(brandIds)
  },

  onToggleCommonBrand(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    const name = String(e.currentTarget.dataset.name || '')
    if (name) this._nameMap[id] = name
    let ids = this.data.brandIds.slice()
    const idx = ids.indexOf(id)
    if (idx >= 0) {
      ids.splice(idx, 1)
    } else {
      ids.push(id)
    }
    this.syncBrandState(ids)
  },

  onPickBrand() {
    const self = this
    wx.navigateTo({
      url: '/pages/sale/brand-pick',
      events: {
        acceptBrands(data: { brandIds: number[]; brandNames: string[] }) {
          const brandIds = (data && data.brandIds) || []
          const brandNames = (data && data.brandNames) || []
          brandIds.forEach((id, i) => {
            if (brandNames[i]) self._nameMap[id] = brandNames[i]
          })
          self.syncBrandState(brandIds)
        },
      },
      success(res) {
        res.eventChannel.emit('initBrands', {
          brandIds: self.data.brandIds.slice(),
        })
      },
    })
  },

  onToggleType(e: WechatMiniprogram.TouchEvent) {
    const code = String(e.currentTarget.dataset.code || '')
    if (!code) return
    const num = Number(code)
    let types = this.data.types.slice()
    const idx = types.indexOf(num)
    if (idx >= 0) {
      types.splice(idx, 1)
    } else {
      types.push(num)
    }
    this.setData({ types })
    this.refreshTypeTags(types)
  },

  onPriceMin(e: WechatMiniprogram.Input) {
    this.setData({ priceMin: e.detail.value || '' })
  },
  onPriceMax(e: WechatMiniprogram.Input) {
    this.setData({ priceMax: e.detail.value || '' })
  },
  onOfficialMin(e: WechatMiniprogram.Input) {
    this.setData({ officialMin: e.detail.value || '' })
  },
  onOfficialMax(e: WechatMiniprogram.Input) {
    this.setData({ officialMax: e.detail.value || '' })
  },

  onNonSale(e: WechatMiniprogram.TouchEvent) {
    const v = e.currentTarget.dataset.v
    let nonSale: number | null = null
    if (v === '1' || v === 1) nonSale = 1
    else if (v === '0' || v === 0) nonSale = 0
    const patch: WechatMiniprogram.IAnyObject = { nonSale }
    if (nonSale === 1) {
      patch.officialMin = ''
      patch.officialMax = ''
    }
    this.setData(patch)
  },

  onReset() {
    this.setData({
      brandIds: [],
      brandNames: [],
      brandSummary: '全部品牌',
      types: [],
      priceMin: '',
      priceMax: '',
      officialMin: '',
      officialMax: '',
      nonSale: null,
    })
    this.refreshTypeTags([])
    this.refreshCommonTags([])
  },

  onConfirm() {
    const sale = normalizePair(parsePrice(this.data.priceMin), parsePrice(this.data.priceMax))
    const nonSale = this.data.nonSale
    const official =
      nonSale === 1
        ? { min: null, max: null }
        : normalizePair(parsePrice(this.data.officialMin), parsePrice(this.data.officialMax))
    const result: SaleFilterResult = {
      brandIds: this.data.brandIds.slice(),
      types: this.data.types.slice(),
      priceMin: sale.min,
      priceMax: sale.max,
      officialPriceMin: official.min,
      officialPriceMax: official.max,
      nonSale,
    }
    if (this._channel) {
      this._channel.emit('acceptFilter', result)
    }
    wx.navigateBack()
  },
})
