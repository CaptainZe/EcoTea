import { getCurrentTenant } from './auth-api'
import { SaleSkuItem } from './sale-api'

/** 本机销售报价行（对齐 H5 chai_sale_quote） */
export interface SaleQuoteItem {
  id: number
  skuCode?: string
  name?: string
  brandName?: string
  title?: string
  salePrice?: number | null
  salePriceShow?: string
  officialPrice?: number | null
  officialPriceShow?: string
  discountShow?: string
  gradeName?: string
  specShow?: string
  prodBatchShow?: string
  expirationName?: string
  coverImage?: string
  spuId?: number | null
  totalQty?: number | null
  qty: number
  quotePrice: number | null
  updatedAt: number
}

interface SaleQuoteState {
  items: SaleQuoteItem[]
  orderDiscount: number
}

function storageKey(): string {
  const t = getCurrentTenant()
  const tid = t && t.tenantId ? t.tenantId : 0
  return 'chai_sale_quote_' + tid
}

function emptyState(): SaleQuoteState {
  return { items: [], orderDiscount: 0 }
}

export function toNum(v: unknown): number | null {
  if (v == null || v === '') return null
  const n = Number(v)
  return isNaN(n) ? null : n
}

function read(): SaleQuoteState {
  try {
    const raw = wx.getStorageSync(storageKey())
    if (!raw) return emptyState()
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!data || !Array.isArray(data.items)) return emptyState()
    let disc = toNum(data.orderDiscount)
    if (disc == null || disc < 0) disc = 0
    data.orderDiscount = Math.round(disc * 100) / 100
    return data as SaleQuoteState
  } catch (e) {
    return emptyState()
  }
}

function write(data: SaleQuoteState): void {
  try {
    wx.setStorageSync(storageKey(), data)
  } catch (e) {
    console.warn('[sale-quote] write fail', e)
  }
}

function snapshotOf(item: SaleSkuItem): Omit<SaleQuoteItem, 'qty' | 'quotePrice' | 'updatedAt'> {
  let cover = item.coverImage || null
  if (!cover && item.imageUrls && item.imageUrls.length) {
    cover = item.imageUrls[0]
  }
  return {
    id: item.id,
    skuCode: item.skuCode || '',
    name: item.name || '',
    brandName: item.brandName || '',
    title: item.title || '',
    salePrice: item.salePrice != null ? item.salePrice : null,
    salePriceShow: item.salePriceShow || '',
    officialPrice: item.officialPrice != null ? item.officialPrice : null,
    officialPriceShow: item.officialPriceShow || '',
    discountShow: item.discountShow || '',
    gradeName: item.gradeName || '',
    specShow: item.specShow || '',
    prodBatchShow: item.prodBatchShow || '',
    expirationName: item.expirationName || '',
    coverImage: cover || undefined,
    spuId: item.spuId != null ? item.spuId : null,
    totalQty: item.totalQty != null ? item.totalQty : null,
  }
}

function findIndex(items: SaleQuoteItem[], id: number | string): number {
  const sid = String(id)
  for (let i = 0; i < items.length; i++) {
    if (String(items[i].id) === sid) return i
  }
  return -1
}

export function displayTitle(it: SaleQuoteItem | SaleSkuItem): string {
  return (
    it.title ||
    [it.brandName, it.name].filter(Boolean).join(' · ') ||
    '商品'
  )
}

export function plainAmount(n: number): string {
  const s = String(Math.round(n * 100) / 100)
  // 仅去掉小数尾零；整数（如 20、2900）原样保留
  if (s.indexOf('.') >= 0) {
    return s.replace(/\.?0+$/, '') || '0'
  }
  return s || '0'
}

export function formatOfficialLine(it: SaleQuoteItem): string {
  const show = it.officialPriceShow || ''
  if (!show || show === '-') return ''
  return String(show).trim()
}

function discountParen(quotePrice: number | null, officialPrice: number | null | undefined): string {
  const q = toNum(quotePrice)
  const o = toNum(officialPrice)
  if (q == null || o == null || o <= 0) return ''
  const d = (q * 10) / o
  const s = (Math.round(d * 100) / 100).toFixed(2).replace(/\.?0+$/, '')
  return '（' + s + '折）'
}

/** 特价：2988元（3.4折） */
export function formatQuotePriceLine(it: SaleQuoteItem): string {
  let price = toNum(it.quotePrice)
  if (price == null) price = toNum(it.salePrice)
  if (price == null) return ''
  let line = plainAmount(price) + '元'
  let disc = discountParen(price, it.officialPrice)
  if (!disc && it.discountShow && price === toNum(it.salePrice)) {
    const ds = String(it.discountShow).trim()
    if (ds) {
      if (ds.indexOf('折') >= 0) {
        disc = '（' + ds.replace(/[（）()]/g, '') + '）'
      } else {
        disc = '（' + ds + '）'
      }
    }
  }
  return line + disc
}

export function kindCount(): number {
  return read().items.length
}

export function getItems(): SaleQuoteItem[] {
  return read().items.slice()
}

export function has(id: number | string | null | undefined): boolean {
  if (id == null) return false
  return findIndex(read().items, id) >= 0
}

export function add(item: SaleSkuItem): boolean {
  if (!item || item.id == null) return false
  const data = read()
  if (findIndex(data.items, item.id) >= 0) return false
  const snap = snapshotOf(item)
  data.items.push({
    ...snap,
    qty: 1,
    quotePrice: toNum(snap.salePrice),
    updatedAt: Date.now(),
  })
  write(data)
  return true
}

export function setQty(id: number | string, qty: number | string): void {
  const q = Number(qty)
  if (!q || isNaN(q) || q < 1) {
    remove(id)
    return
  }
  const data = read()
  const idx = findIndex(data.items, id)
  if (idx < 0) return
  data.items[idx].qty = Math.floor(q)
  data.items[idx].updatedAt = Date.now()
  write(data)
}

export function setQuotePrice(id: number | string, price: unknown): void {
  const data = read()
  const idx = findIndex(data.items, id)
  if (idx < 0) return
  let n = toNum(price)
  if (n == null || n < 0) n = toNum(data.items[idx].salePrice)
  data.items[idx].quotePrice = n
  data.items[idx].updatedAt = Date.now()
  write(data)
}

export function remove(id: number | string): void {
  const data = read()
  data.items = data.items.filter((it) => String(it.id) !== String(id))
  write(data)
}

/** 换同款：保留数量，报价重置为新参考售价 */
export function replace(oldId: number | string, newItem: SaleSkuItem): boolean {
  if (oldId == null || !newItem || newItem.id == null) return false
  const data = read()
  const oldIdx = findIndex(data.items, oldId)
  if (oldIdx < 0) return false
  if (String(newItem.id) === String(oldId)) return false
  if (findIndex(data.items, newItem.id) >= 0) return false
  const keepQty = Math.max(1, Math.floor(Number(data.items[oldIdx].qty) || 1))
  const snap = snapshotOf(newItem)
  data.items[oldIdx] = {
    ...snap,
    qty: keepQty,
    quotePrice: toNum(snap.salePrice),
    updatedAt: Date.now(),
  }
  write(data)
  return true
}

export function clear(): void {
  write(emptyState())
}

export function lineAmount(it: SaleQuoteItem): number {
  let price = toNum(it.quotePrice)
  if (price == null) price = toNum(it.salePrice)
  if (price == null) return 0
  return price * (Number(it.qty) || 1)
}

export function subtotalAmount(): number {
  let sum = 0
  getItems().forEach((it) => {
    sum += lineAmount(it)
  })
  return Math.round(sum * 100) / 100
}

export function getOrderDiscount(): number {
  const data = read()
  let disc = toNum(data.orderDiscount)
  if (disc == null || disc < 0) return 0
  const sub = subtotalAmount()
  if (disc > sub) disc = sub
  return Math.round(disc * 100) / 100
}

export function setOrderDiscount(v: unknown): void {
  const data = read()
  let disc = toNum(v)
  if (disc == null || disc < 0) disc = 0
  const sub = subtotalAmount()
  if (disc > sub) disc = sub
  data.orderDiscount = Math.round(disc * 100) / 100
  write(data)
}

export function payableAmount(): number {
  return Math.round(Math.max(0, subtotalAmount() - getOrderDiscount()) * 100) / 100
}

/** 对齐 H5 buildQuoteText */
export function formatQuotePlainText(): string {
  const items = getItems()
  if (!items.length) return ''
  const blocks: string[] = ['【销售报价单】']
  let totalPcs = 0
  items.forEach((it) => {
    const lines = [displayTitle(it)]
    if (it.gradeName) lines.push('等级：' + it.gradeName)
    if (it.specShow) lines.push('规格：' + it.specShow)
    if (it.prodBatchShow) lines.push('批次：' + it.prodBatchShow)
    if (it.expirationName) lines.push('保质期：' + it.expirationName)
    const official = formatOfficialLine(it)
    if (official) lines.push('官方：' + official)
    const sale = formatQuotePriceLine(it)
    if (sale) lines.push('特价：' + sale)
    const qty = it.qty || 1
    totalPcs += qty
    lines.push('数量：' + qty)
    blocks.push(lines.join('\n'))
  })
  let foot =
    '------------------------------\n共 ' +
    items.length +
    ' 种，合计 ' +
    totalPcs +
    ' 件'
  const disc = getOrderDiscount()
  const sub = subtotalAmount()
  const pay = payableAmount()
  if (disc > 0) {
    foot +=
      '\n小计：¥' +
      plainAmount(sub) +
      '\n优惠：−¥' +
      plainAmount(disc) +
      '\n应付合计：¥' +
      plainAmount(pay)
  } else {
    foot += '\n金额合计：¥' + plainAmount(pay)
  }
  blocks.push(foot)
  return blocks.join('\n\n')
}
