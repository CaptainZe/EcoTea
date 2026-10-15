import { request, ApiResult } from '../utils/request'

export interface BrandOption {
  id: number
  name: string
}

/** 品牌选页单项（常用 / 字母分组共用） */
export interface BrandPickerItem {
  id: number
  name: string
  nameInitial: string
  namePinyin?: string
  logo?: string
}

export interface BrandPickerGroup {
  initial: string
  items: BrandPickerItem[]
}

/**
 * 品牌选页：常用快捷区 + 全量字母分组 + 右侧索引。
 * 常用品牌仍出现在 groups 中。
 */
export interface BrandPicker {
  common: BrandPickerItem[]
  groups: BrandPickerGroup[]
  /** 有常用时前插「常」，再跟各组 initial */
  index: string[]
}

export interface DictOption {
  code: string
  text: string
}

export interface WarehouseOption {
  id: number
  name?: string
  shortName?: string
}

/** H5 同款：扁平 id/name，列表筛选用 */
export function fetchOnlineBrands(): Promise<ApiResult<BrandOption[]>> {
  return request<BrandOption[]>({
    url: '/chai/brand/online',
    method: 'GET',
    auth: true,
  })
}

/** 品牌选页专用：常用 + 字母分组 + 索引 */
export function fetchBrandPicker(): Promise<ApiResult<BrandPicker>> {
  return request<BrandPicker>({
    url: '/chai/brand/online-picker',
    method: 'GET',
    auth: true,
  })
}

export function fetchChaiTypes(): Promise<ApiResult<DictOption[]>> {
  return request<DictOption[]>({
    url: '/chai/dict/options',
    method: 'GET',
    auth: true,
    data: { label: 'CHAI_TYPE' },
  })
}

export function fetchOnlineWarehouses(): Promise<ApiResult<WarehouseOption[]>> {
  return request<WarehouseOption[]>({
    url: '/chai/warehouse/online',
    method: 'GET',
    auth: true,
  })
}
