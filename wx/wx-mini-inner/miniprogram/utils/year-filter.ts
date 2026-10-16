/** 与 admin ChaiProdBatch 一致：CHAI_PROD_BATCH */
export const PROD_BATCH_FIRST = 100
export const PROD_BATCH_SECOND = 200

/** picker：全部 / 上半年 / 下半年（首项清空）；芯片未选仍显示「上下年」 */
export const PROD_BATCH_SHEET_LABELS = ['全部', '上半年', '下半年']

/** 年份快捷筛选：解析弹窗输入，空串表示清空 */
export type YearParseResult =
  | { ok: true; year: number | null }
  | { ok: false; message: string }

export function parseYearInput(raw: string): YearParseResult {
  const s = (raw || '').trim()
  if (!s) {
    return { ok: true, year: null }
  }
  if (!/^\d{4}$/.test(s)) {
    return { ok: false, message: '请输入4位年份' }
  }
  const year = Number(s)
  const max = new Date().getFullYear() + 1
  if (year < 1900 || year > max) {
    return { ok: false, message: '年份超出范围' }
  }
  return { ok: true, year }
}

export function yearChipLabel(year: number | null | undefined): string {
  return year != null ? year + '年' : '年份'
}

export function prodBatchChipLabel(prodBatch: number | null | undefined): string {
  if (prodBatch === PROD_BATCH_FIRST) return '上半年'
  if (prodBatch === PROD_BATCH_SECOND) return '下半年'
  return '上下年'
}

/** ActionSheet tapIndex → prodBatch；取消或非法为 null */
export function prodBatchFromSheetIndex(tapIndex: number): number | null {
  if (tapIndex === 1) return PROD_BATCH_FIRST
  if (tapIndex === 2) return PROD_BATCH_SECOND
  return null
}
