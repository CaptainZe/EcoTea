import { SaleSkuItem } from '../services/sale-api'

export const DEFAULT_PACK_LINE = '原封 原盒 原标 原袋'

/** 半角括号规范为全角 */
export function formatPriceLineForCopy(show?: string): string {
  if (!show) {
    return ''
  }
  return String(show).trim().replace(/\(/g, '（').replace(/\)/g, '）')
}

export function displaySaleTitle(item: SaleSkuItem): string {
  if (item.title) {
    return String(item.title).trim()
  }
  const brand = item.brandName ? String(item.brandName).trim() : ''
  const name = item.name ? String(item.name).trim() : ''
  if (brand && name) {
    return brand + ' · ' + name
  }
  return brand || name || '商品'
}

export function defaultOfficialCopy(item: SaleSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '非卖品'
  }
  return formatPriceLineForCopy(item.officialPriceShow)
}

/** 官方价数字（用于折率）；非卖品或无效返回 null */
export function officialPriceNum(item: SaleSkuItem): number | null {
  if (!item.officialPriceShow || item.officialPriceShow === '-' || item.officialPriceShow === '非卖品') {
    if (item.officialPrice != null) {
      const n = Number(item.officialPrice)
      return !isNaN(n) && n > 0 ? n : null
    }
    return null
  }
  if (item.officialPrice != null) {
    const n = Number(item.officialPrice)
    if (!isNaN(n) && n > 0) return n
  }
  const raw = String(item.officialPriceShow).replace(/元$/, '').replace(/[¥￥]/g, '').trim()
  const n = Number(raw)
  return !isNaN(n) && n > 0 ? n : null
}

export function defaultSalePriceNum(item: SaleSkuItem): string {
  if (item.salePrice != null) {
    const n = Number(item.salePrice)
    if (!isNaN(n)) return String(n)
  }
  if (item.salePriceShow) {
    const raw = String(item.salePriceShow)
      .replace(/\(.*$/, '')
      .replace(/（.*$/, '')
      .replace(/元$/, '')
      .replace(/[¥￥]/g, '')
      .trim()
    const n = Number(raw)
    if (!isNaN(n)) return String(n)
  }
  return ''
}

/** 去掉小数尾零；整数（如 2900）原样保留 */
function trimTrailingZeros(num: number): string {
  const rounded = Math.round(num * 100) / 100
  let s = String(rounded)
  if (s.indexOf('.') >= 0) {
    s = s.replace(/\.?0+$/, '')
  }
  return s || '0'
}

/** 售价数字 + 官方价 → 售价：2988元（3.4折） */
export function formatSalePriceWithDiscount(saleNum: string | number, official: number | null): string {
  const n = typeof saleNum === 'number' ? saleNum : Number(String(saleNum || '').trim())
  if (saleNum === '' || saleNum == null || isNaN(n) || n < 0) {
    return ''
  }
  const plain = trimTrailingZeros(n)
  let line = plain + '元'
  if (official != null && official > 0) {
    const d = (n * 10) / official
    const ds = trimTrailingZeros(Math.round(d * 100) / 100)
    line += '（' + ds + '折）'
  }
  return line
}

/** 库存件数（文案数量默认） */
export function stockQtyText(item: SaleSkuItem): string {
  let qty = item.totalQty != null ? Number(item.totalQty) : 0
  if (isNaN(qty) || qty < 0) qty = 0
  return String(Math.floor(qty))
}

export interface SaleCopyDraft {
  packLine: string
  /** 售价数字（不含元/折） */
  salePriceNum: string
  qty: string
  remark: string
}

export function buildSaleCopyDraft(item: SaleSkuItem): SaleCopyDraft {
  return {
    packLine: DEFAULT_PACK_LINE,
    salePriceNum: defaultSalePriceNum(item),
    qty: stockQtyText(item),
    remark: '',
  }
}

/**
 * 客户向文案：标题 / 等级 / 规格 / 原封行 / 生产日期 / 保质期 / 官方 / 售价 / 数量 / 备注
 */
export function formatSaleCopyText(item: SaleSkuItem, draft?: Partial<SaleCopyDraft>): string {
  const d = {
    ...buildSaleCopyDraft(item),
    ...(draft || {}),
  }
  const lines: string[] = [displaySaleTitle(item)]
  const push = (label: string, value?: string | null) => {
    const v = value != null ? String(value).trim() : ''
    if (v) {
      lines.push(label + '：' + v)
    }
  }
  push('等级', item.gradeName)
  push('规格', item.specShow)
  const pack = (d.packLine || '').trim()
  if (pack) {
    lines.push(pack)
  }
  push('生产日期', item.prodBatchShow)
  push('保质期', item.expirationName)
  push('官方', defaultOfficialCopy(item))
  const saleLine = formatSalePriceWithDiscount(d.salePriceNum, officialPriceNum(item))
  push('售价', saleLine)
  const qty = (d.qty || '').trim() || stockQtyText(item)
  lines.push('数量：' + qty)
  const remark = (d.remark || '').trim()
  if (remark) {
    lines.push('备注：' + remark)
  }
  return lines.join('\n')
}
