import { resolveLaunchTarget, reLaunchTo } from '../../services/session'

Page({
  data: {
    tip: '正在检查登录状态…',
  },

  onLoad() {
    resolveLaunchTarget().then((target) => {
      console.log('[E1.5] launch ->', target)
      reLaunchTo(target)
    })
  },
})
