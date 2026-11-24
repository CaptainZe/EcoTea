import { InfoSkuItem } from '../services/info-api'

export function formatSalePlain(item: InfoSkuItem): string {
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

export function formatRecyclePlain(item: InfoSkuItem): string {
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

export function formatOfficialShort(item: InfoSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '官方价 非卖品'
  }
  return '官方价 ¥' + String(item.officialPriceShow).replace(/元$/, '')
}

export function formatOfficialPlain(item: InfoSkuItem): string {
  if (!item.officialPriceShow || item.officialPriceShow === '-') {
    return ''
  }
  if (item.officialPriceShow === '非卖品') {
    return '官方价 非卖品'
  }
  return '官方价 ¥' + String(item.officialPriceShow).replace(/元$/, '')
}

/** 破损价 · 无提袋扣（价侧，非库存） */
export function recycleExtraLine(item: InfoSkuItem): string {
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

export function buildTags(item: InfoSkuItem): string[] {
  const tags: string[] = []
  if (item.gradeName) tags.push(String(item.gradeName))
  if (item.specShow) tags.push(String(item.specShow))
  if (item.prodBatchShow) tags.push(String(item.prodBatchShow))
  if (item.expirationName) tags.push(String(item.expirationName))
  return tags
}

export type InfoListRow = InfoSkuItem & {
  officialShort: string
  salePlain: string
  saleDiscountShow: string
  recyclePlain: string
  recycleDiscountShow: string
  extraLine: string
  tags: string[]
  showSame: boolean
}

export function toListRow(item: InfoSkuItem): InfoListRow {
  return {
    ...item,
    officialShort: formatOfficialShort(item),
    salePlain: formatSalePlain(item),
    saleDiscountShow: item.discountShow || '',
    recyclePlain: formatRecyclePlain(item),
    recycleDiscountShow: item.recycleDiscountShow || '',
    extraLine: recycleExtraLine(item),
    tags: buildTags(item),
    showSame: !!(item.spuId && item.sameSpuCount != null && item.sameSpuCount > 1),
  }
}
