import { request, ApiResult } from '../utils/request'

/** 与 server-api ChaiSkuSaleItemVO 对齐（销售侧，无回收价） */
export interface SaleSkuItem {
  id: number
  spuId?: number
  skuCode?: string
  title?: string
  name?: string
  brandName?: string
  gradeName?: string
  specShow?: string
  prodBatchShow?: string
  expirationName?: string
  salePrice?: number
  officialPrice?: number
  officialPriceShow?: string
  salePriceShow?: string
  discountShow?: string
  totalQty?: number
  qtyNoBag?: number
  qtyDamaged?: number
  qtyDamagedNoBag?: number
  whShortNames?: string[]
  warehouseStocks?: Array<{
    whId?: number
    shortName?: string
    qty?: number
    qtyNoBag?: number
    qtyDamaged?: number
    qtyDamagedNoBag?: number
  }>
  sameSpuSaleCount?: number
  coverImage?: string
  imageUrls?: string[]
}

export interface SaleSkuPage {
  total: number
  page: number
  size: number
  matchType?: string
  keyword?: string
  barcode?: string
  spuId?: number
  list: SaleSkuItem[]
}

export interface SaleListQuery {
  keyword?: string
  barcode?: string
  spuId?: number
  whId?: number
  recycleRecent?: number
  brandIds?: number[]
  types?: number[]
  priceMin?: number | null
  priceMax?: number | null
  officialPriceMin?: number | null
  officialPriceMax?: number | null
  nonSale?: number | null
  year?: number | null
  prodBatch?: number | null
  includeWh?: number
  page?: number
  size?: number
}

export function fetchSaleList(params: SaleListQuery): Promise<ApiResult<SaleSkuPage>> {
  const data: WechatMiniprogram.IAnyObject = {
    page: params.page || 1,
    size: params.size || 20,
    includeWh: params.includeWh != null ? params.includeWh : 1,
  }
  if (params.barcode) {
    data.barcode = params.barcode
  } else if (params.keyword) {
    data.keyword = params.keyword
  }
  if (params.spuId) {
    data.spuId = params.spuId
  }
  if (params.whId) {
    data.whId = params.whId
  }
  if (params.recycleRecent) {
    data.recycleRecent = 1
  }
  if (params.brandIds && params.brandIds.length) {
    data.brandIds = params.brandIds.join(',')
  }
  if (params.types && params.types.length) {
    data.types = params.types.join(',')
  }
  if (params.priceMin != null) {
    data.priceMin = params.priceMin
  }
  if (params.priceMax != null) {
    data.priceMax = params.priceMax
  }
  if (params.nonSale != null) {
    data.nonSale = params.nonSale
  }
  if (params.nonSale !== 1) {
    if (params.officialPriceMin != null) {
      data.officialPriceMin = params.officialPriceMin
    }
    if (params.officialPriceMax != null) {
      data.officialPriceMax = params.officialPriceMax
    }
  }
  if (params.year != null) {
    data.year = params.year
  }
  if (params.prodBatch != null) {
    data.prodBatch = params.prodBatch
  }
  return request<SaleSkuPage>({
    url: '/chai/sku/sale/list',
    method: 'GET',
    auth: true,
    data,
  })
}

export function fetchSaleDetail(id: number): Promise<ApiResult<SaleSkuItem>> {
  return request<SaleSkuItem>({
    url: '/chai/sku/sale/detail',
    method: 'GET',
    auth: true,
    data: { id },
  })
}
