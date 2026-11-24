import { request, ApiResult } from '../utils/request'

/** 与 server-api ChaiSkuInfoItemVO 对齐（无库存） */
export interface InfoSkuItem {
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
  salePrice?: number
  salePriceShow?: string
  discountShow?: string
  recyclePrice?: number
  recyclePriceShow?: string
  recycleDiscountShow?: string
  recyclePriceReducePer?: number
  recycleDamagePrice?: number
  recycleDamagePriceShow?: string
  recyclePriceReduceNoBag?: number
  recyclePriceReduceNoBagShow?: string
  sameSpuCount?: number
  coverImage?: string
  imageUrls?: string[]
}

export interface InfoSkuPage {
  total: number
  page: number
  size: number
  matchType?: string
  keyword?: string
  barcode?: string
  spuId?: number
  list: InfoSkuItem[]
}

export interface InfoListQuery {
  keyword?: string
  barcode?: string
  spuId?: number
  brandIds?: number[]
  types?: number[]
  priceMin?: number | null
  priceMax?: number | null
  recyclePriceMin?: number | null
  recyclePriceMax?: number | null
  officialPriceMin?: number | null
  officialPriceMax?: number | null
  nonSale?: number | null
  year?: number | null
  prodBatch?: number | null
  page?: number
  size?: number
}

export function fetchInfoList(params: InfoListQuery): Promise<ApiResult<InfoSkuPage>> {
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
  if (params.priceMin != null) {
    data.priceMin = params.priceMin
  }
  if (params.priceMax != null) {
    data.priceMax = params.priceMax
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
  if (params.year != null) {
    data.year = params.year
  }
  if (params.prodBatch != null) {
    data.prodBatch = params.prodBatch
  }
  return request<InfoSkuPage>({
    url: '/chai/sku/info/list',
    method: 'GET',
    auth: true,
    data,
  })
}

export function fetchInfoDetail(id: number): Promise<ApiResult<InfoSkuItem>> {
  return request<InfoSkuItem>({
    url: '/chai/sku/info/detail',
    method: 'GET',
    auth: true,
    data: { id },
  })
}
