import { getCurrentTenant } from './auth-api'
import { RecycleSkuItem } from './recycle-api'

/**
 * 本机回收报价行。
 * 存储键 chai_recycle_quote_{tenantId}，与销售篮 chai_sale_quote_* 完全隔离。
 */
export interface RecycleQuoteItem {
  id: number
  skuCode?: string
  name?: string
  brandName?: string
  title?: string
  recyclePrice?: number | null
  recyclePriceShow?: string
  officialPrice?: number | null
  officialPriceShow?: string
  recycleDiscountShow?: string
  recycleDamagePriceShow?: string
  recyclePriceReducePer?: number | null
  recyclePriceReduceNoBagShow?: string
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

interface RecycleQuoteState {
  items: RecycleQuoteItem[]
  /** 整单加价（对齐 H5 orderAdd；应付 = 小计 + 加价） */
  orderAdd: number
}

function storageKey(): string {
  const t = getCurrentTenant()
  const tid = t && t.tenantId ? t.tenantId : 0
  return 'chai_recycle_quote_' + tid
}

function emptyState(): RecycleQuoteState {
  return { items: [], orderAdd: 0 }
}

export function toNum(v: unknown): number | null {
  if (v == null || v === '') return null
  const n = Number(v)
  return isNaN(n) ? null : n
}

function read(): RecycleQuoteState {
  try {
    const raw = wx.getStorageSync(storageKey())
    if (!raw) return emptyState()
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!data || !Array.isArray(data.items)) return emptyState()
    let add = toNum(data.orderAdd)
    if (add == null || add < 0) add = 0
    data.orderAdd = Math.round(add * 100) / 100
    delete data.orderDiscount
    return data as RecycleQuoteState
  } catch (e) {
    return emptyState()
  }
}

function write(data: RecycleQuoteState): void {
  try {
    wx.setStorageSync(storageKey(), data)
  } catch (e) {
    console.warn('[recycle-quote] write fail', e)
  }
}

function snapshotOf(item: RecycleSkuItem): Omit<RecycleQuoteItem, 'qty' | 'quotePrice' | 'updatedAt'> {
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
    recyclePrice: item.recyclePrice != null ? item.recyclePrice : null,
    recyclePriceShow: item.recyclePriceShow || '',
    officialPrice: item.officialPrice != null ? item.officialPrice : null,
    officialPriceShow: item.officialPriceShow || '',
    recycleDiscountShow: item.recycleDiscountShow || '',
    recycleDamagePriceShow: item.recycleDamagePriceShow || '',
    recyclePriceReducePer: item.recyclePriceReducePer != null ? item.recyclePriceReducePer : null,
    recyclePriceReduceNoBagShow: item.recyclePriceReduceNoBagShow || '',
    gradeName: item.gradeName || '',
    specShow: item.specShow || '',
    prodBatchShow: item.prodBatchShow || '',
    expirationName: item.expirationName || '',
    coverImage: cover || undefined,
    spuId: item.spuId != null ? item.spuId : null,
    totalQty: item.totalQty != null ? item.totalQty : null,
  }
}

function findIndex(items: RecycleQuoteItem[], id: number | string): number {
  const sid = String(id)
  for (let i = 0; i < items.length; i++) {
    if (String(items[i].id) === sid) return i
  }
  return -1
}

export function displayTitle(it: RecycleQuoteItem | RecycleSkuItem): string {
  return (
    it.title ||
    [it.brandName, it.name].filter(Boolean).join(' · ') ||
    '商品'
  )
}

export function plainAmount(n: number): string {
  const s = String(Math.round(n * 100) / 100)
  if (s.indexOf('.') >= 0) {
    return s.replace(/\.?0+$/, '') || '0'
  }
  return s || '0'
}

export function kindCount(): number {
  return read().items.length
}

export function getItems(): RecycleQuoteItem[] {
  return read().items.slice()
}

export function has(id: number | string | null | undefined): boolean {
  if (id == null) return false
  return findIndex(read().items, id) >= 0
}

export function add(item: RecycleSkuItem): boolean {
  if (!item || item.id == null) return false
  const data = read()
  if (findIndex(data.items, item.id) >= 0) return false
  const snap = snapshotOf(item)
  data.items.push({
    ...snap,
    qty: 1,
    quotePrice: toNum(snap.recyclePrice),
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
  if (n == null || n < 0) n = toNum(data.items[idx].recyclePrice)
  data.items[idx].quotePrice = n
  data.items[idx].updatedAt = Date.now()
  write(data)
}

export function remove(id: number | string): void {
  const data = read()
  data.items = data.items.filter((it) => String(it.id) !== String(id))
  write(data)
}

export function replace(oldId: number | string, newItem: RecycleSkuItem): boolean {
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
    quotePrice: toNum(snap.recyclePrice),
    updatedAt: Date.now(),
  }
  write(data)
  return true
}

export function clear(): void {
  write(emptyState())
}

export function formatOfficialLine(it: RecycleQuoteItem): string {
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

/** 回收报价：268元（3.8折） */
export function formatQuotePriceLine(it: RecycleQuoteItem): string {
  let price = toNum(it.quotePrice)
  if (price == null) price = toNum(it.recyclePrice)
  if (price == null) return ''
  let line = plainAmount(price) + '元'
  let disc = discountParen(price, it.officialPrice)
  if (!disc && it.recycleDiscountShow && price === toNum(it.recyclePrice)) {
    const ds = String(it.recycleDiscountShow).trim()
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

export function lineAmount(it: RecycleQuoteItem): number {
  let price = toNum(it.quotePrice)
  if (price == null) price = toNum(it.recyclePrice)
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

export function getOrderAdd(): number {
  const data = read()
  let add = toNum(data.orderAdd)
  if (add == null || add < 0) return 0
  return Math.round(add * 100) / 100
}

/** 设置整单加价。空/非法视为 0（对齐 H5，不加上限） */
export function setOrderAdd(v: unknown): void {
  const data = read()
  let n = toNum(v)
  if (n == null || n < 0) n = 0
  data.orderAdd = Math.round(n * 100) / 100
  write(data)
}

/** 应付 = 小计 + 整单加价 */
export function payableAmount(): number {
  return Math.round((subtotalAmount() + getOrderAdd()) * 100) / 100
}

/** 回收报价纯文本 */
export function formatQuotePlainText(): string {
  const items = getItems()
  if (!items.length) return ''
  const blocks: string[] = ['【回收报价单】']
  let totalPcs = 0
  items.forEach((it) => {
    const lines = [displayTitle(it)]
    if (it.gradeName) lines.push('等级：' + it.gradeName)
    if (it.specShow) lines.push('规格：' + it.specShow)
    if (it.prodBatchShow) lines.push('批次：' + it.prodBatchShow)
    if (it.expirationName) lines.push('保质期：' + it.expirationName)
    const official = formatOfficialLine(it)
    if (official) lines.push('官方：' + official)
    const recycle = formatQuotePriceLine(it)
    if (recycle) lines.push('回收价：' + recycle)
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
  const add = getOrderAdd()
  const sub = subtotalAmount()
  const pay = payableAmount()
  if (add > 0) {
    foot +=
      '\n小计：¥' +
      plainAmount(sub) +
      '\n加价：+¥' +
      plainAmount(add) +
      '\n应付合计：¥' +
      plainAmount(pay)
  } else {
    foot += '\n金额合计：¥' + plainAmount(pay)
  }
  blocks.push(foot)
  return blocks.join('\n\n')
}
