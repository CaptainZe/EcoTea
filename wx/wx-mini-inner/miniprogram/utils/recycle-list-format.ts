import { RecycleSkuItem } from '../services/recycle-api'

/** 与 H5 formatRecyclePlain 对齐 */
export function formatRecyclePlain(item: RecycleSkuItem): string {
  if (item.recyclePrice != null) {
    const n = Number(item.recyclePrice)
    if (!isNaN(n)) {
      return '¥ ' + String(n)
    }
  }
  if (item.recyclePriceShow) {
    return String(item.recyclePriceShow).replace(/\(.*$/, '').trim()
  }
  return '-'
}

export function formatOfficialShort(item: RecycleSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '官方 非卖品'
  }
  return '官方 ¥' + String(item.officialPriceShow).replace(/元$/, '')
}

export function formatOfficialPlain(item: RecycleSkuItem): string {
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
      extras: stockExtras(row as RecycleSkuItem),
    })
  }
  return rows
}

/** 破损价 · 无提袋扣 */
export function recycleExtraLine(item: RecycleSkuItem): string {
  const parts: string[] = []
  if (item.recycleDamagePriceShow) {
    let dmg = String(item.recycleDamagePriceShow)
    if (item.recyclePriceReducePer != null && String(item.recyclePriceReducePer) !== '') {
      dmg += '(' + item.recyclePriceReducePer + '%)'
    }
    parts.push('破损 ' + dmg)
  }
  const noBag = item.recyclePriceReduceNoBagShow
  if (noBag != null && String(noBag).trim() !== '') {
    parts.push('无提袋扣 ¥' + String(noBag).trim())
  }
  return parts.join(' · ')
}

/** 不要求有货：库存 n 件；≤3 稀缺色 */
export function formatStockLabel(item: RecycleSkuItem): { text: string; scarce: boolean } {
  let qty = item.totalQty != null ? Number(item.totalQty) : 0
  if (isNaN(qty) || qty < 0) {
    qty = 0
  }
  return {
    scarce: qty <= 3,
    text: '库存 ' + qty + ' 件',
  }
}

export function stockExtras(item: RecycleSkuItem): string[] {
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

export function buildTags(item: RecycleSkuItem): string[] {
  const tags: string[] = []
  if (item.gradeName) tags.push(String(item.gradeName))
  if (item.specShow) tags.push(String(item.specShow))
  if (item.prodBatchShow) tags.push(String(item.prodBatchShow))
  if (item.expirationName) tags.push(String(item.expirationName))
  return tags
}

export type RecycleListRow = RecycleSkuItem & {
  recyclePlain: string
  officialShort: string
  discountShow: string
  extraLine: string
  stockText: string
  stockScarce: boolean
  stockExtras: string[]
  tags: string[]
  showSame: boolean
  inQuote: boolean
}

export function toListRow(item: RecycleSkuItem, inQuote = false): RecycleListRow {
  const stock = formatStockLabel(item)
  return {
    ...item,
    recyclePlain: formatRecyclePlain(item),
    officialShort: formatOfficialShort(item),
    discountShow: item.recycleDiscountShow || '',
    extraLine: recycleExtraLine(item),
    stockText: stock.text,
    stockScarce: stock.scarce,
    stockExtras: stockExtras(item),
    tags: buildTags(item),
    showSame: !!(item.spuId && item.sameSpuCount != null && item.sameSpuCount > 1),
    inQuote,
  }
}
