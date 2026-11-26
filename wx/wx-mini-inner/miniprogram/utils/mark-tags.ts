/** SPU/SKU 标记展示（与 Admin 列表色系对齐） */

export type MarkTag = {
  key: string
  text: string
  kind: 'hide' | 'core' | 'taste'
}

export function buildMarkTags(item: {
  hideExternal?: number | null
  coreFlag?: number | null
  tastingFlag?: number | null
}): MarkTag[] {
  const tags: MarkTag[] = []
  if (Number(item.hideExternal) === 1) {
    tags.push({ key: 'hide', text: '对外隐藏', kind: 'hide' })
  }
  if (Number(item.coreFlag) === 1) {
    tags.push({ key: 'core', text: '核心', kind: 'core' })
  }
  if (Number(item.tastingFlag) === 1) {
    tags.push({ key: 'taste', text: '品鉴', kind: 'taste' })
  }
  return tags
}

export function hasAnyMark(item: {
  hideExternal?: number | null
  coreFlag?: number | null
  tastingFlag?: number | null
}): boolean {
  return buildMarkTags(item).length > 0
}
