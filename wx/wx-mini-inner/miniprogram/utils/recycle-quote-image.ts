import { getReporterName } from '../services/auth-api'
import {
  displayTitle,
  formatOfficialLine,
  formatQuotePriceLine,
  getOrderAdd,
  getItems,
  payableAmount,
  plainAmount,
  RecycleQuoteItem,
  subtotalAmount,
} from '../services/recycle-quote'

import {
  COLOR_BG_CARD,
  COLOR_PRICE,
  COLOR_RECYCLE,
  COLOR_RECYCLE_SOFT,
  COLOR_TEXT_PRIMARY,
  COLOR_TEXT_SECONDARY,
} from '../styles/token-colors'

const W = 750
const PAD = 28
const SHEET_BG = COLOR_RECYCLE
const CARD_BG = COLOR_BG_CARD
const PRICE = COLOR_PRICE
const TEXT = COLOR_TEXT_PRIMARY
const MUTED = COLOR_TEXT_SECONDARY
const ACCENT_SOFT = COLOR_RECYCLE_SOFT
const LABEL_W = 88
/** 单页最多 SKU 数，避免离屏 canvas 高度超限 */
const PAGE_SIZE = 8
const HEAD_H = 128
const CONTINUE_FOOT_H = 100

function loadImage(canvas: WechatMiniprogram.Canvas, src: string): Promise<WechatMiniprogram.Image> {
  return new Promise((resolve, reject) => {
    wx.getImageInfo({
      src,
      success: (info) => {
        const img = canvas.createImage()
        img.onload = () => resolve(img)
        img.onerror = () => reject(new Error('img load fail'))
        img.src = info.path
      },
      fail: () => reject(new Error('getImageInfo fail')),
    })
  })
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const chars = String(text || '').split('')
  const lines: string[] = []
  let line = ''
  for (const ch of chars) {
    const test = line + ch
    if (ctx.measureText(test).width > maxWidth && line) {
      lines.push(line)
      line = ch
    } else {
      line = test
    }
  }
  if (line) lines.push(line)
  return lines.length ? lines : ['']
}

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

function dateStr(): string {
  const now = new Date()
  const p = (n: number) => (n < 10 ? '0' + n : String(n))
  return now.getFullYear() + '-' + p(now.getMonth() + 1) + '-' + p(now.getDate())
}

function chunkItems<T>(items: T[], size: number): T[][] {
  const pages: T[][] = []
  for (let i = 0; i < items.length; i += size) {
    pages.push(items.slice(i, i + size))
  }
  return pages
}

type LayoutCard = {
  item: RecycleQuoteItem
  titleLines: string[]
  rows: Array<{ k: string; v: string; emphasize?: boolean }>
  cover: WechatMiniprogram.Image | null
  height: number
}

type OrderTotals = {
  kindCount: number
  pcs: number
  add: number
  sub: number
  pay: number
}

function buildCards(
  ctx: CanvasRenderingContext2D,
  items: RecycleQuoteItem[],
  covers: Array<WechatMiniprogram.Image | null>,
  textAreaW: number,
  coverS: number
): LayoutCard[] {
  return items.map((item, idx) => {
    const titleLines = wrapText(ctx, displayTitle(item), textAreaW)
    const rows: Array<{ k: string; v: string; emphasize?: boolean }> = []
    if (item.gradeName) rows.push({ k: '等级', v: item.gradeName })
    if (item.specShow) rows.push({ k: '规格', v: item.specShow })
    if (item.prodBatchShow) rows.push({ k: '批次', v: item.prodBatchShow })
    if (item.expirationName) rows.push({ k: '保质期', v: item.expirationName })
    const official = formatOfficialLine(item)
    if (official) rows.push({ k: '官方', v: official })
    const recycle = formatQuotePriceLine(item)
    if (recycle) rows.push({ k: '回收价', v: recycle, emphasize: true })
    rows.push({ k: '数量', v: String(item.qty || 1) })
    const titleH = Math.max(40, titleLines.length * 36)
    const rowsH = rows.length * 36
    const bodyH = titleH + 14 + rowsH
    const height = Math.max(coverS + 40, bodyH + 36)
    return { item, titleLines, rows, cover: covers[idx], height }
  })
}

function exportCanvas(
  canvasNode: WechatMiniprogram.Canvas,
  dpr: number,
  H: number
): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.canvasToTempFilePath({
      canvas: canvasNode,
      x: 0,
      y: 0,
      width: W,
      height: H,
      destWidth: W * dpr,
      destHeight: H * dpr,
      fileType: 'png',
      success: (res) => resolve(res.tempFilePath),
      fail: (err) => reject(new Error((err && err.errMsg) || '导出失败')),
    })
  })
}

function renderOnePage(
  canvasNode: WechatMiniprogram.Canvas,
  ctx: CanvasRenderingContext2D,
  dpr: number,
  pageItems: RecycleQuoteItem[],
  pageCovers: Array<WechatMiniprogram.Image | null>,
  pageIndex: number,
  pageCount: number,
  reporter: string,
  totals: OrderTotals
): Promise<string> {
  // 上一页可能残留 dpr scale，量字前先复位
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.font = '28px sans-serif'
  const contentW = W - PAD * 2
  const coverS = 112
  const textAreaW = contentW - coverS - 36
  const cards = buildCards(ctx, pageItems, pageCovers, textAreaW, coverS)
  const isLast = pageIndex === pageCount - 1
  const footH = isLast ? (totals.add > 0 ? 268 : 220) : CONTINUE_FOOT_H

  let listH = 0
  cards.forEach((c) => {
    listH += c.height + 16
  })
  const H = HEAD_H + listH + footH + PAD

  canvasNode.width = W * dpr
  canvasNode.height = H * dpr
  ctx.scale(dpr, dpr)

  ctx.fillStyle = SHEET_BG
  ctx.fillRect(0, 0, W, H)

  const titleBase = '回收报价单'
  const title =
    pageCount > 1 ? titleBase + ' (' + (pageIndex + 1) + '/' + pageCount + ')' : titleBase
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 42px sans-serif'
  ctx.fillText(title, PAD, 56)
  ctx.fillStyle = 'rgba(255,255,255,0.9)'
  ctx.font = '24px sans-serif'
  const ds = dateStr()
  const metaY = 98
  const reporterLine = reporter ? '报单人：' + reporter : ''
  if (reporterLine) {
    ctx.fillText(reporterLine, PAD, metaY)
  }
  ctx.fillText(ds, W - PAD - ctx.measureText(ds).width, metaY)

  let y = HEAD_H
  cards.forEach((card) => {
    const cardH = card.height
    ctx.fillStyle = CARD_BG
    roundRect(ctx, PAD, y, contentW, cardH, 14)
    ctx.fill()

    ctx.fillStyle = SHEET_BG
    roundRect(ctx, PAD, y + 14, 8, cardH - 28, 4)
    ctx.fill()

    const coverX = PAD + 24
    const coverY = y + 20
    if (card.cover) {
      ctx.save()
      roundRect(ctx, coverX, coverY, coverS, coverS, 10)
      ctx.clip()
      ctx.drawImage(card.cover as unknown as CanvasImageSource, coverX, coverY, coverS, coverS)
      ctx.restore()
    } else {
      ctx.fillStyle = ACCENT_SOFT
      roundRect(ctx, coverX, coverY, coverS, coverS, 10)
      ctx.fill()
      ctx.fillStyle = SHEET_BG
      ctx.font = '28px sans-serif'
      ctx.fillText('茶', coverX + 42, coverY + 66)
    }

    let ty = y + 32
    const textX = coverX + coverS + 18
    const textMax = W - PAD - 16 - textX
    ctx.fillStyle = TEXT
    ctx.font = 'bold 28px sans-serif'
    card.titleLines.forEach((ln) => {
      ctx.fillText(ln, textX, ty)
      ty += 34
    })
    ty += 10
    card.rows.forEach((row) => {
      ctx.fillStyle = MUTED
      ctx.font = '24px sans-serif'
      ctx.fillText(row.k, textX, ty)
      const maxV = textMax - LABEL_W
      let v = row.v
      ctx.font = row.emphasize ? 'bold 26px sans-serif' : '24px sans-serif'
      if (ctx.measureText(v).width > maxV) {
        while (v.length && ctx.measureText(v + '…').width > maxV) {
          v = v.slice(0, -1)
        }
        v += '…'
      }
      ctx.fillStyle = row.emphasize ? PRICE : TEXT
      ctx.fillText(v, textX + LABEL_W, ty)
      ty += 36
    })

    y += cardH + 16
  })

  const footBoxH = footH - 20
  ctx.fillStyle = ACCENT_SOFT
  roundRect(ctx, PAD, y, contentW, footBoxH, 14)
  ctx.fill()

  if (!isLast) {
    ctx.fillStyle = MUTED
    ctx.font = '26px sans-serif'
    const tip = '续下页'
    ctx.fillText(tip, (W - ctx.measureText(tip).width) / 2, y + footBoxH / 2 + 10)
  } else {
    let fy = y + 48
    ctx.fillStyle = MUTED
    ctx.font = '26px sans-serif'
    ctx.fillText('共 ' + totals.kindCount + ' 种，合计 ' + totals.pcs + ' 件', PAD + 28, fy)
    fy += 46
    if (totals.add > 0) {
      ctx.fillStyle = MUTED
      ctx.font = '26px sans-serif'
      ctx.fillText('小计', PAD + 28, fy)
      const subS = '¥' + plainAmount(totals.sub)
      ctx.fillText(subS, W - PAD - 28 - ctx.measureText(subS).width, fy)
      fy += 42
      ctx.fillStyle = PRICE
      ctx.fillText('加价', PAD + 28, fy)
      const addS = '+¥' + plainAmount(totals.add)
      ctx.fillText(addS, W - PAD - 28 - ctx.measureText(addS).width, fy)
      fy += 48
    }
    ctx.fillStyle = TEXT
    ctx.font = 'bold 30px sans-serif'
    ctx.fillText(totals.add > 0 ? '应付' : '合计', PAD + 28, fy)
    ctx.fillStyle = PRICE
    ctx.font = 'bold 36px sans-serif'
    const payS = '¥' + plainAmount(totals.pay)
    ctx.fillText(payS, W - PAD - 28 - ctx.measureText(payS).width, fy)
    fy += 44
    ctx.fillStyle = MUTED
    ctx.font = '22px sans-serif'
    ctx.fillText('内部回收参考报价 · 以沟通确认为准', PAD + 28, fy)
  }

  return exportCanvas(canvasNode, dpr, H)
}

/**
 * 绘制回收报价长图并导出临时路径（琥珀底 + 白卡片）。
 * 超过 PAGE_SIZE 时分页多张：多页标题带 (当前/总页)；非末页「续下页」；末页整单合计。
 */
export async function renderRecycleQuoteImageFile(
  canvasId: string,
  component: WechatMiniprogram.Component.TrivialInstance | WechatMiniprogram.Page.TrivialInstance
): Promise<string[]> {
  const items = getItems()
  if (!items.length) {
    throw new Error('报价单为空')
  }

  const canvasNode = await new Promise<WechatMiniprogram.Canvas>((resolve, reject) => {
    component
      .createSelectorQuery()
      .select('#' + canvasId)
      .fields({ node: true, size: true })
      .exec((res) => {
        if (res && res[0] && res[0].node) {
          resolve(res[0].node as WechatMiniprogram.Canvas)
        } else {
          reject(new Error('canvas 未就绪'))
        }
      })
  })

  const ctx = canvasNode.getContext('2d') as unknown as CanvasRenderingContext2D
  const dpr = wx.getSystemInfoSync().pixelRatio || 2
  const reporter = getReporterName()

  const covers: Array<WechatMiniprogram.Image | null> = []
  for (const it of items) {
    if (it.coverImage) {
      try {
        covers.push(await loadImage(canvasNode, it.coverImage))
      } catch (e) {
        covers.push(null)
      }
    } else {
      covers.push(null)
    }
  }

  let pcs = 0
  items.forEach((it) => {
    pcs += it.qty || 1
  })
  const totals: OrderTotals = {
    kindCount: items.length,
    pcs,
    add: getOrderAdd(),
    sub: subtotalAmount(),
    pay: payableAmount(),
  }

  const pages = chunkItems(items, PAGE_SIZE)
  const pageCount = pages.length
  const paths: string[] = []

  for (let pi = 0; pi < pageCount; pi++) {
    if (pageCount > 1) {
      wx.showLoading({ title: '正在生成 ' + (pi + 1) + '/' + pageCount + '…', mask: true })
    }
    const pageItems = pages[pi]
    const coverStart = pi * PAGE_SIZE
    const pageCovers = covers.slice(coverStart, coverStart + pageItems.length)
    const path = await renderOnePage(
      canvasNode,
      ctx,
      dpr,
      pageItems,
      pageCovers,
      pi,
      pageCount,
      reporter,
      totals
    )
    paths.push(path)
  }

  return paths
}
