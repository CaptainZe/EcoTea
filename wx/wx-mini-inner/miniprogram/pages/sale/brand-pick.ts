import {
  BrandPickerGroup,
  BrandPickerItem,
  fetchBrandPicker,
} from '../../services/chai-meta-api'

type PickItem = BrandPickerItem & { on: boolean; letter: string }
type PickGroup = { initial: string; secId: string; items: PickItem[] }
type SecOffset = { key: string; top: number }

function secIdOf(key: string): string {
  if (key === '#') return 'sec_hash'
  return 'sec_' + key
}

/** 选品牌页只要字母索引，常用已移到筛选页 */
function letterIndexOnly(index: string[]): string[] {
  return (index || []).filter((k) => k && k !== '常')
}

function letterOf(b: BrandPickerItem): string {
  const initial = (b.nameInitial || '').trim()
  if (initial && initial !== '#') return initial.charAt(0).toUpperCase()
  const name = (b.name || '').trim()
  return name ? name.charAt(0) : '?'
}

function toPickItem(b: BrandPickerItem, on: boolean): PickItem {
  return { ...b, on, letter: letterOf(b) }
}

function matchBrand(b: BrandPickerItem, kw: string): boolean {
  if (!kw) return true
  const name = (b.name || '').toLowerCase()
  const py = (b.namePinyin || '').toLowerCase()
  return name.indexOf(kw) >= 0 || py.indexOf(kw) >= 0
}

Page({
  data: {
    loading: true,
    keyword: '',
    searching: false,
    searchList: [] as PickItem[],

    groups: [] as PickGroup[],
    indexList: [] as string[],
    activeIndex: '',
    scrollTop: 0,
    scrollAnim: true,
    indexBubble: '',
    selectedCount: 0,
  },

  _channel: null as WechatMiniprogram.EventChannel | null,
  _selected: new Set<number>(),
  _nameMap: {} as Record<number, string>,
  _allItems: [] as BrandPickerItem[],
  _groupsRaw: [] as BrandPickerGroup[],
  _offsets: [] as SecOffset[],
  _currentScrollTop: 0,
  _measureTimer: 0 as number,
  _bubbleTimer: 0 as number,
  _indexing: false,

  onLoad() {
    const channel = this.getOpenerEventChannel()
    this._channel = channel
    channel.on('initBrands', (payload: { brandIds?: number[] }) => {
      const ids = (payload && payload.brandIds) || []
      this._selected = new Set(ids.filter((id) => !!id))
      this.setData({ selectedCount: this._selected.size })
      this.applySelectionToView()
    })
    this.loadPicker()
  },

  loadPicker() {
    this.setData({ loading: true })
    fetchBrandPicker()
      .then((res) => {
        if (res.code !== 0 || !res.data) {
          wx.showToast({ title: res.message || '加载失败', icon: 'none' })
          this.setData({ loading: false, groups: [], indexList: [] })
          return
        }
        const groupsRaw = res.data.groups || []
        const indexList = letterIndexOnly(res.data.index || [])

        const nameMap: Record<number, string> = {}
        const all: BrandPickerItem[] = []
        groupsRaw.forEach((g) => {
          ;(g.items || []).forEach((b) => {
            if (b.id != null) nameMap[b.id] = b.name
            all.push(b)
          })
        })
        this._nameMap = nameMap
        this._allItems = all
        this._groupsRaw = groupsRaw

        this.setData({
          loading: false,
          indexList,
          activeIndex: indexList[0] || '',
          selectedCount: this._selected.size,
        })
        this.applySelectionToView()
        this.scheduleMeasure()
      })
      .catch((err: Error) => {
        this.setData({ loading: false })
        wx.showToast({ title: (err && err.message) || '网络错误', icon: 'none' })
      })
  },

  applySelectionToView() {
    const selected = this._selected
    const groups: PickGroup[] = this._groupsRaw.map((g) => ({
      initial: g.initial,
      secId: secIdOf(g.initial),
      items: (g.items || []).map((b) => toPickItem(b, selected.has(b.id))),
    }))
    const patch: WechatMiniprogram.IAnyObject = {
      groups,
      selectedCount: selected.size,
    }
    if (this.data.searching) {
      patch.searchList = this.buildSearchList(this.data.keyword)
    }
    this.setData(patch)
  },

  buildSearchList(keyword: string): PickItem[] {
    const kw = (keyword || '').trim().toLowerCase()
    const selected = this._selected
    return this._allItems
      .filter((b) => matchBrand(b, kw))
      .map((b) => toPickItem(b, selected.has(b.id)))
  },

  onKeyword(e: WechatMiniprogram.Input) {
    const keyword = e.detail.value || ''
    const kw = keyword.trim()
    if (!kw) {
      this.setData({ keyword, searching: false, searchList: [] })
      this.scheduleMeasure()
      return
    }
    this.setData({
      keyword,
      searching: true,
      searchList: this.buildSearchList(kw),
    })
  },

  onClearKeyword() {
    this.setData({ keyword: '', searching: false, searchList: [] })
    this.scheduleMeasure()
  },

  onToggle(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    const name = String(e.currentTarget.dataset.name || '')
    if (this._selected.has(id)) {
      this._selected.delete(id)
    } else {
      this._selected.add(id)
      if (name) this._nameMap[id] = name
    }
    this.applySelectionToView()
  },

  onTapIndex(e: WechatMiniprogram.TouchEvent) {
    const key = String(e.currentTarget.dataset.key || '')
    if (!key) return
    this.jumpToKey(key, true)
  },

  onIndexTouchStart(e: WechatMiniprogram.TouchEvent) {
    this._indexing = true
    this.pickIndexByTouch(e)
  },

  onIndexTouchMove(e: WechatMiniprogram.TouchEvent) {
    if (!this._indexing) return
    this.pickIndexByTouch(e)
  },

  onIndexTouchEnd() {
    this._indexing = false
    this.hideBubbleSoon()
  },

  pickIndexByTouch(e: WechatMiniprogram.TouchEvent) {
    const touch = e.touches && e.touches[0]
    if (!touch) return
    const query = wx.createSelectorQuery()
    query.selectAll('.index-item').boundingClientRect()
    query.exec((res) => {
      const rects = (res && res[0]) as WechatMiniprogram.BoundingClientRectCallbackResult[]
      if (!rects || !rects.length) return
      const y = touch.clientY
      let key = this.data.indexList[0] || ''
      for (let i = 0; i < rects.length; i++) {
        const r = rects[i]
        if (y >= r.top && y <= r.bottom) {
          key = this.data.indexList[i]
          break
        }
        if (y < r.top) {
          key = this.data.indexList[Math.max(0, i - 1)]
          break
        }
        key = this.data.indexList[i]
      }
      if (key) this.jumpToKey(key, false)
    })
  },

  jumpToKey(key: string, anim: boolean) {
    this.showBubble(key)
    this.setData({ activeIndex: key })
    const go = (top: number) => {
      // 相同 scrollTop 不会触发滚动，微调一下
      let next = top
      if (Math.abs(this._currentScrollTop - top) < 1) {
        next = top + 1
      }
      this.setData({ scrollAnim: !!anim, scrollTop: next })
      this._currentScrollTop = top
    }
    const hit = this._offsets.find((o) => o.key === key)
    if (!hit) {
      this.measureOffsets(() => {
        const again = this._offsets.find((o) => o.key === key)
        if (again) go(again.top)
      })
      return
    }
    go(hit.top)
  },

  scheduleMeasure() {
    if (this.data.searching || this.data.loading) return
    if (this._measureTimer) clearTimeout(this._measureTimer)
    this._measureTimer = setTimeout(() => {
      this.measureOffsets()
    }, 80) as unknown as number
  },

  measureOffsets(done?: () => void) {
    const query = wx.createSelectorQuery()
    query.select('.main .list-scroll').boundingClientRect()
    query.select('.main .list-scroll').scrollOffset()
    query.selectAll('.sec-anchor').fields({
      id: true,
      rect: true,
      dataset: true,
    })
    query.exec((res) => {
      const viewRect = res && res[0]
      const scroll = res && res[1]
      const secs = (res && res[2]) as Array<{
        id?: string
        top?: number
        dataset?: { key?: string }
      }>
      if (!viewRect || !secs || !secs.length) {
        if (done) done()
        return
      }
      const scrollTop = (scroll && scroll.scrollTop) || 0
      const offsets: SecOffset[] = []
      secs.forEach((node) => {
        let k = (node.dataset && node.dataset.key) || ''
        if (!k && node.id) {
          if (node.id === 'sec_hash') k = '#'
          else if (node.id.indexOf('sec_') === 0) k = node.id.slice(4)
        }
        if (!k || node.top == null) return
        offsets.push({
          key: k,
          top: Math.max(0, Math.round(node.top - viewRect.top + scrollTop)),
        })
      })
      offsets.sort((a, b) => a.top - b.top)
      this._offsets = offsets
      if (done) done()
    })
  },

  onListScroll(e: WechatMiniprogram.ScrollViewScroll) {
    this._currentScrollTop = e.detail.scrollTop || 0
    if (this._indexing || this.data.searching || !this._offsets.length) return
    const y = this._currentScrollTop + 8
    let active = this._offsets[0].key
    for (let i = 0; i < this._offsets.length; i++) {
      if (this._offsets[i].top <= y) {
        active = this._offsets[i].key
      } else {
        break
      }
    }
    if (active !== this.data.activeIndex) {
      this.setData({ activeIndex: active })
    }
  },

  showBubble(key: string) {
    this.setData({ indexBubble: key })
    if (this._bubbleTimer) clearTimeout(this._bubbleTimer)
    this._bubbleTimer = setTimeout(() => {
      if (!this._indexing) this.setData({ indexBubble: '' })
    }, 600) as unknown as number
  },

  hideBubbleSoon() {
    if (this._bubbleTimer) clearTimeout(this._bubbleTimer)
    this._bubbleTimer = setTimeout(() => {
      this.setData({ indexBubble: '' })
    }, 200) as unknown as number
  },

  onReset() {
    this._selected.clear()
    this.applySelectionToView()
  },

  onConfirm() {
    const brandIds = Array.from(this._selected)
    const brandNames = brandIds.map((id) => this._nameMap[id] || '').filter(Boolean)
    if (this._channel) {
      this._channel.emit('acceptBrands', { brandIds, brandNames })
    }
    wx.navigateBack()
  },
})
