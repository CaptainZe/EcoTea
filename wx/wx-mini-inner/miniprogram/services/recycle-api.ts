import { request, ApiResult } from '../utils/request'

/** 与 server-api ChaiSkuRecycleItemVO 对齐 */
export interface RecycleSkuItem {
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
  officialPrice?: number
  officialPriceShow?: string
  recyclePrice?: number
  recyclePriceShow?: string
  recycleDiscountShow?: string
  recyclePriceReducePer?: number
  recycleDamagePrice?: number
  recycleDamagePriceShow?: string
  recyclePriceReduceNoBag?: number
  recyclePriceReduceNoBagShow?: string
  totalQty?: number
  qtyNoBag?: number
  qtyDamaged?: number
  qtyDamagedNoBag?: number
  warehouseStocks?: Array<{
    whId?: number
    shortName?: string
    qty?: number
    qtyNoBag?: number
    qtyDamaged?: number
    qtyDamagedNoBag?: number
  }>
  sameSpuCount?: number
  coverImage?: string
  imageUrls?: string[]
}

export interface RecycleSkuPage {
  total: number
  page: number
  size: number
  matchType?: string
  keyword?: string
  barcode?: string
  spuId?: number
  list: RecycleSkuItem[]
}

export interface RecycleListQuery {
  keyword?: string
  barcode?: string
  spuId?: number
  brandIds?: number[]
  types?: number[]
  recyclePriceMin?: number | null
  recyclePriceMax?: number | null
  officialPriceMin?: number | null
  officialPriceMax?: number | null
  nonSale?: number | null
  page?: number
  size?: number
}

export function fetchRecycleList(params: RecycleListQuery): Promise<ApiResult<RecycleSkuPage>> {
  const data: WechatMiniprogram.IAnyObject = {
    page: params.page || 1,
    size: params.size || 20,
  }
  if (params.barcode) {
    data.barcode = params.barcode
  } else if (params.keyword) {
    data.keyword = params.keyword
  }
  if (params.spuId) {
    data.spuId = params.spuId
  }
  if (params.brandIds && params.brandIds.length) {
    data.brandIds = params.brandIds.join(',')
  }
  if (params.types && params.types.length) {
    data.types = params.types.join(',')
  }
  if (params.recyclePriceMin != null) {
    data.recyclePriceMin = params.recyclePriceMin
  }
  if (params.recyclePriceMax != null) {
    data.recyclePriceMax = params.recyclePriceMax
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
  return request<RecycleSkuPage>({
    url: '/chai/sku/recycle/list',
    method: 'GET',
    auth: true,
    data,
  })
}

export function fetchRecycleDetail(id: number): Promise<ApiResult<RecycleSkuItem>> {
  return request<RecycleSkuItem>({
    url: '/chai/sku/recycle/detail',
    method: 'GET',
    auth: true,
    data: { id },
  })
}
