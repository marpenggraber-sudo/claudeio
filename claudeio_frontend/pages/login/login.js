const app = getApp()

Page({
  data: {
    account: '',
    password: '',
    cookie: '',
    loading: false,
    mode: 'login',
  },

  onAccountInput(e) {
    this.setData({ account: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onCookieInput(e) {
    this.setData({ cookie: e.detail.value })
  },

  submit() {
    const { account, password, cookie, mode } = this.data
    if (!cookie.trim()) {
      wx.showToast({ title: '请先填写 Cookie', icon: 'none' })
      return
    }

    // 添加日志，检查 cookie 长度
    console.log('Cookie 长度:', cookie.length)
    console.log('Cookie 内容（前50字符）:', cookie.substring(0, 50))

    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.apiBase}/${mode === 'register' ? 'register' : 'login'}`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: { account, password, cookie },
      success: res => {
        console.log('后端响应:', res.data)
        const userId = res.data?.userId
        if (userId) {
          wx.setStorageSync('music_userId', userId)
          app.globalData.isLoggedIn = true
          wx.showToast({ title: mode === 'register' ? '注册成功' : '登录成功', icon: 'success' })
          setTimeout(() => {
            wx.navigateTo({ url: '/pages/index/index' })
          }, 500)
        } else {
          wx.showModal({ title: mode === 'register' ? '注册失败' : '登录失败', content: res.data?.message || 'Cookie 验证失败', showCancel: false })
        }
      },
      fail: err => {
        console.error('请求失败:', err)
        wx.showModal({ title: '请求失败', content: err?.errMsg || '网络错误', showCancel: false })
      },
      complete: () => this.setData({ loading: false }),
    })
  },

  switchMode() {
    this.setData({ mode: this.data.mode === 'login' ? 'register' : 'login' })
  }
})
