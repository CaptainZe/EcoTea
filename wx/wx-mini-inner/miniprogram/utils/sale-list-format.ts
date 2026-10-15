import { SaleSkuItem } from '../services/sale-api'

/** 与 H5 formatSalePlain 对齐 */
export function formatSalePlain(item: SaleSkuItem): string {
  if (item.salePrice != null) {
    const n = Number(item.salePrice)
    if (!isNaN(n)) {
      return '¥ ' + String(n)
    }
  }
  if (item.salePriceShow) {
    return String(item.salePriceShow).replace(/\(.*$/, '').trim()
  }
  return '询价'
}

/** 与 H5 列表 formatOfficialShort 对齐 */
export function formatOfficialShort(item: SaleSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '官方 非卖品'
  }
  return '官方 ¥' + String(item.officialPriceShow).replace(/元$/, '')
}

/** 与 H5 详情 formatOfficialPlain 对齐 */
export function formatOfficialPlain(item: SaleSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '官方价 非卖品'
  }
  return '官方价 ¥' + String(item.officialPriceShow).replace(/元$/, '')
}

export type WhStockRow = {
  whId?: number
  shortName: string
  qty: number
  extras: string[]
}

/** 分仓库存行（仅 qty>0），含例外品相 */
export function buildWhStockRows(
  stocks?: Array<{
    whId?: number
    shortName?: string
    qty?: number
    qtyNoBag?: number
    qtyDamaged?: number
    qtyDamagedNoBag?: number
  }>
): WhStockRow[] {
  if (!stocks || !stocks.length) {
    return []
  }
  const rows: WhStockRow[] = []
  for (const row of stocks) {
    if (!row || row.qty == null || Number(row.qty) <= 0) {
      continue
    }
    const name = row.shortName || ''
    if (!name) {
      continue
    }
    rows.push({
      whId: row.whId,
      shortName: name,
      qty: Number(row.qty),
      extras: stockExtras(row as SaleSkuItem),
    })
  }
  return rows
}

/** 与 H5 ChaiSaleStock 对齐：≤3 仅剩；>3 现货 */
export function formatStockLabel(item: SaleSkuItem): { text: string; scarce: boolean } {
  let qty = item.totalQty != null ? Number(item.totalQty) : 0
  if (isNaN(qty) || qty < 0) {
    qty = 0
  }
  const scarce = qty <= 3
  return {
    scarce,
    text: scarce ? '仅剩 ' + qty + ' 件' : '现货 ' + qty + ' 件',
  }
}

export function stockExtras(item: SaleSkuItem): string[] {
  const extras: string[] = []
  if (item.qtyNoBag != null && Number(item.qtyNoBag) > 0) {
    extras.push('无袋 ' + item.qtyNoBag)
  }
  if (item.qtyDamaged != null && Number(item.qtyDamaged) > 0) {
    extras.push('破损 ' + item.qtyDamaged)
  }
  if (item.qtyDamagedNoBag != null && Number(item.qtyDamagedNoBag) > 0) {
    extras.push('破损无袋 ' + item.qtyDamagedNoBag)
  }
  return extras
}

export function buildTags(item: SaleSkuItem): string[] {
  const tags: string[] = []
  if (item.gradeName) tags.push(String(item.gradeName))
  if (item.specShow) tags.push(String(item.specShow))
  if (item.prodBatchShow) tags.push(String(item.prodBatchShow))
  if (item.expirationName) tags.push(String(item.expirationName))
  return tags
}

export type SaleListRow = SaleSkuItem & {
  salePlain: string
  officialShort: string
  discountShow?: string
  stockText: string
  stockScarce: boolean
  stockExtras: string[]
  tags: string[]
  whShortNames: string[]
  imageCount: number
  showSame: boolean
  inQuote: boolean
}

export function toListRow(item: SaleSkuItem, inQuote = false): SaleListRow {
  const stock = formatStockLabel(item)
  const urls = item.imageUrls && item.imageUrls.length
    ? item.imageUrls
    : item.coverImage
      ? [item.coverImage]
      : []
  return {
    ...item,
    salePlain: formatSalePlain(item),
    officialShort: formatOfficialShort(item),
    discountShow: item.discountShow || '',
    stockText: stock.text,
    stockScarce: stock.scarce,
    stockExtras: stockExtras(item),
    tags: buildTags(item),
    whShortNames: item.whShortNames || [],
    imageCount: urls.length,
    showSame: !!(item.spuId && item.sameSpuSaleCount != null && item.sameSpuSaleCount > 1),
    inQuote,
  }
}
