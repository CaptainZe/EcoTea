/** 确保相册写权限 */
export function ensureAlbumAuth(): Promise<void> {
  return new Promise((resolve, reject) => {
    wx.getSetting({
      success(setting) {
        if (setting.authSetting['scope.writePhotosAlbum']) {
          resolve()
          return
        }
        wx.authorize({
          scope: 'scope.writePhotosAlbum',
          success: () => resolve(),
          fail: () => {
            wx.showModal({
              title: '需要相册权限',
              content: '请在设置中允许保存图片到相册',
              confirmText: '去设置',
              success(modal) {
                if (!modal.confirm) {
                  reject(new Error('未授权相册'))
                  return
                }
                wx.openSetting({
                  success(res) {
                    if (res.authSetting['scope.writePhotosAlbum']) {
                      resolve()
                    } else {
                      reject(new Error('未授权相册'))
                    }
                  },
                  fail: () => reject(new Error('打开设置失败')),
                })
              },
            })
          },
        })
      },
      fail: () => reject(new Error('读取设置失败')),
    })
  })
}

function downloadFile(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('下载失败 ' + (res.statusCode || '')))
        }
      },
      fail: (err) => reject(new Error((err && err.errMsg) || '下载失败')),
    })
  })
}

function saveImage(path: string): Promise<void> {
  return new Promise((resolve, reject) => {
    wx.saveImageToPhotosAlbum({
      filePath: path,
      success: () => resolve(),
      fail: (err) => reject(new Error((err && err.errMsg) || '保存失败')),
    })
  })
}

/**
 * 将图片 URL 列表逐张保存到相册。返回成功/失败数量。
 */
export async function saveImagesToAlbum(
  urls: string[]
): Promise<{ ok: number; fail: number }> {
  const list = (urls || []).filter((u) => !!u && String(u).trim())
  if (!list.length) {
    return { ok: 0, fail: 0 }
  }
  await ensureAlbumAuth()
  let ok = 0
  let fail = 0
  for (const url of list) {
    try {
      const path = await downloadFile(String(url).trim())
      await saveImage(path)
      ok += 1
    } catch (e) {
      fail += 1
      console.warn('[album] save fail', url, e)
    }
  }
  return { ok, fail }
}

/** 将本地临时文件保存到相册 */
export async function saveLocalImageToAlbum(filePath: string): Promise<void> {
  if (!filePath) {
    throw new Error('无图片')
  }
  await ensureAlbumAuth()
  await saveImage(filePath)
}
