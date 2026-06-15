const app = getApp()

Page({
  data: {
    // 登录方式: 
    loginMethod: 'qrcode',

    // 手动登录字段
    account: '',
    password: '',
    cookie: '',

    // 二维码登录字段
    qrCodeKey: '',           // 二维码 Key
    qrCodeImg: '',           // 二维码图片 base64
    qrCodeStatus: '请生成二维码开始登录',
    qrCodeExpired: false,
    qrCodePolling: null,     // 轮询定时器

    loading: false,
    mode: 'login', // 'login' | 'register'

    // Cookie 自动刷新配置
    cookieCheckInterval: null,
    cookieCheckIntervalMs: 30 * 60 * 1000, // 30分钟检查一次
  },

  onLoad() {
    // 页面加载时启动 Cookie 自动检查
    this.startCookieAutoCheck()
  },

  onUnload() {
    // 页面卸载时停止定时器
    this.stopCookieAutoCheck()
    this.stopQrCodePolling()
  },

  // ============ 登录方式切换 ============
  switchLoginMethod(e) {
    const method = e.currentTarget.dataset.value
    console.log('切换登录方式:', method)
    
    this.setData({
      loginMethod: method,
      loading: false,
      // 新增逻辑：如果是扫码登录，强制将模式重置为 'login'，保持头部状态和业务逻辑正确
      mode: method === 'qrcode' ? 'login' : this.data.mode
    })
  },

  switchNeteaseLoginType(e) {
    const type = e.currentTarget.dataset.value
    console.log('切换网易云登录类型:', type)
    this.setData({ neteaseLoginType: type })
  },

  // ============ 手动登录输入 ============
  onAccountInput(e) {
    this.setData({ account: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onCookieInput(e) {
    this.setData({ cookie: e.detail.value })
  },

  // ============ 手动登录/注册 ============
  submit() {
    const { account, password, cookie, mode } = this.data

    if (!cookie.trim()) {
      wx.showToast({
        title: '请先填写 Cookie',
        icon: 'none'
      })
      return
    }

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
          // 保存用户 ID 和 Cookie
          wx.setStorageSync('music_userId', userId)
          wx.setStorageSync('netease_cookie', cookie)
          wx.setStorageSync('netease_cookie_timestamp', Date.now())

          app.globalData.isLoggedIn = true

          wx.showToast({
            title: mode === 'register' ? '注册成功' : '登录成功',
            icon: 'success'
          })

          setTimeout(() => {
            wx.navigateBack({ delta: 1 })
          }, 500)
        } else {
          wx.showModal({
            title: mode === 'register' ? '注册失败' : '登录失败',
            content: res.data?.message || 'Cookie 验证失败',
            showCancel: false
          })
        }
      },
      fail: err => {
        console.error('请求失败:', err)
        wx.showModal({
          title: '请求失败',
          content: err?.errMsg || '网络错误',
          showCancel: false
        })
      },
      complete: () => this.setData({ loading: false }),
    })
  },

  // ============ Cookie 自动刷新 ============

  /**
   * 启动 Cookie 自动检查定时器
   */
  startCookieAutoCheck() {
    // 清除之前的定时器
    this.stopCookieAutoCheck()

    // 立即检查一次
    this.checkCookieStatus()

    // 设置定时检查（30分钟）
    const intervalId = setInterval(() => {
      this.checkCookieStatus()
    }, this.data.cookieCheckIntervalMs)

    this.setData({ cookieCheckInterval: intervalId })
    console.log('Cookie 自动检查已启动，间隔:', this.data.cookieCheckIntervalMs / 1000 / 60, '分钟')
  },

  /**
   * 停止 Cookie 自动检查定时器
   */
  stopCookieAutoCheck() {
    if (this.data.cookieCheckInterval) {
      clearInterval(this.data.cookieCheckInterval)
      this.setData({ cookieCheckInterval: null })
      console.log('Cookie 自动检查已停止')
    }
  },

  /**
   * 检查 Cookie 状态
   */
  async checkCookieStatus() {
    const cookie = wx.getStorageSync('netease_cookie')

    if (!cookie) {
      console.log('Cookie 检查: 无 Cookie')
      return
    }

    try {
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: `${app.globalData.apiBase}/cookie-status`,
          method: 'GET',
          data: { cookie },
          success: resolve,
          fail: reject
        })
      })

      console.log('Cookie 状态检查结果:', res.data)

      if (res.data.valid) {
        console.log('Cookie 有效 ✅')
        // 更新检查时间戳
        wx.setStorageSync('netease_cookie_last_check', Date.now())
      } else {
        console.log('Cookie 已失效 ❌')
        this.handleCookieExpired()
      }

    } catch (error) {
      console.error('Cookie 状态检查失败:', error)
    }
  },

  /**
   * 处理 Cookie 失效
   */
  handleCookieExpired() {
    // 清除失效的 Cookie
    wx.removeStorageSync('netease_cookie')
    wx.removeStorageSync('netease_cookie_timestamp')

    // 提示用户重新登录
    wx.showModal({
      title: 'Cookie 已失效',
      content: '请重新登录网易云音乐获取新的 Cookie',
      confirmText: '去登录',
      cancelText: '稍后',
      success: (res) => {
        if (res.confirm) {
          // 切换到网易云登录方式
          this.setData({ loginMethod: 'netease' })
        }
      }
    })
  },

  /**
   * 手动触发 Cookie 检查（调试用）
   */
  manualCheckCookie() {
    wx.showLoading({ title: '检查中...' })
    this.checkCookieStatus().finally(() => {
      wx.hideLoading()
    })
  },

  // ============ 登录/注册模式切换 ============
  switchMode() {
    this.setData({
      mode: this.data.mode === 'login' ? 'register' : 'login'
    })
  },

  // ============ 二维码登录 ============

  /**
   * 生成二维码
   */
  async generateQrCode() {
    // 停止之前的轮询
    this.stopQrCodePolling()

    this.setData({
      loading: true,
      qrCodeImg: '',
      qrCodeExpired: false,
      qrCodeStatus: '正在生成二维码...'
    })

    try {
      // Step 1: 获取二维码 Key
      const keyRes = await new Promise((resolve, reject) => {
        wx.request({
          url: `${app.globalData.apiBase}/qr-key`,
          method: 'GET',
          success: resolve,
          fail: reject
        })
      })

      if (!keyRes.data.success) {
        throw new Error(keyRes.data.errorMessage || '获取二维码 Key 失败')
      }

      const qrKey = keyRes.data.unikey

      // Step 2: 生成二维码图片
      const imgRes = await new Promise((resolve, reject) => {
        wx.request({
          url: `${app.globalData.apiBase}/qr-create?key=${qrKey}`,
          method: 'GET',
          success: resolve,
          fail: reject
        })
      })

      if (!imgRes.data.success) {
        throw new Error(imgRes.data.errorMessage || '生成二维码失败')
      }

      // 显示二维码
      this.setData({
        qrCodeKey: qrKey,
        qrCodeImg: imgRes.data.qrimg,
        qrCodeStatus: '请使用网易云音乐 APP 扫码'
      })

      // 开始轮询检查状态
      this.startQrCodePolling()

    } catch (error) {
      console.error('生成二维码失败:', error)
      wx.showModal({
        title: '生成失败',
        content: error.message || '网络错误',
        showCancel: false
      })
      this.setData({ qrCodeStatus: '生成失败，请重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 开始轮询检查二维码状态
   */
  startQrCodePolling() {
    this.stopQrCodePolling()

    const checkStatus = async () => {
      try {
        const res = await new Promise((resolve, reject) => {
          wx.request({
            url: `${app.globalData.apiBase}/qr-check?key=${this.data.qrCodeKey}`,
            method: 'GET',
            success: resolve,
            fail: reject
          })
        })

        const { code, message, cookie } = res.data

        switch (code) {
          case 800:
            // 二维码过期
            this.stopQrCodePolling()
            this.setData({
              qrCodeStatus: '二维码已过期',
              qrCodeExpired: true
            })
            break

          case 801:
            // 等待扫码
            this.setData({ qrCodeStatus: '等待扫码中...' })
            break

          case 802:
            // 已扫码，等待确认
            this.setData({ qrCodeStatus: '✅ 已扫码，请在手机上确认' })
            break

          case 803:
            // 登录成功
            this.stopQrCodePolling()
            this.handleQrLoginSuccess(cookie)
            break

          default:
            console.warn('未知状态码:', code, message)
        }

      } catch (error) {
        console.error('检查二维码状态失败:', error)
        // 不中断轮询，继续尝试
      }
    }

    // 立即检查一次
    checkStatus()

    // 每2秒检查一次
    const pollInterval = setInterval(checkStatus, 2000)
    this.setData({ qrCodePolling: pollInterval })
  },

  /**
   * 停止轮询
   */
  stopQrCodePolling() {
    if (this.data.qrCodePolling) {
      clearInterval(this.data.qrCodePolling)
      this.setData({ qrCodePolling: null })
    }
  },

  /**
   * 处理二维码登录成功
   */
  async handleQrLoginSuccess(cookie) {
    if (!cookie) {
      wx.showModal({
        title: '登录失败',
        content: '未获取到 Cookie',
        showCancel: false
      })
      return
    }

    wx.showLoading({
      title: '登录中...',
      mask: true
    })

    try {
      // 调用后端完成登录（自动注册/更新用户）
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: `${app.globalData.apiBase}/qr-login?cookie=${encodeURIComponent(cookie)}`,
          method: 'POST',
          success: resolve,
          fail: reject
        })
      })

      wx.hideLoading()

      if (res.statusCode === 200 && res.data.userId) {
        // 登录成功
        const { userId, nickname } = res.data

        // 保存登录信息
        wx.setStorageSync('netease_cookie', cookie)
        wx.setStorageSync('netease_cookie_timestamp', Date.now())
        wx.setStorageSync('user_id', userId)
        wx.setStorageSync('music_userId', userId)
        wx.setStorageSync('user_nickname', nickname)

        // 更新全局状态
        app.globalData.userId = userId
        app.globalData.userNickname = nickname

        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        this.setData({
          qrCodeStatus: '✅ 登录成功'
        })

        // 1秒后跳转到首页
        setTimeout(() => {
          wx.navigateBack({ delta: 1 })
        }, 1000)

      } else {
        // 登录失败
        wx.showModal({
          title: '登录失败',
          content: res.data.message || '请重试',
          showCancel: false
        })
      }

    } catch (error) {
      wx.hideLoading()
      console.error('二维码登录失败:', error)
      wx.showModal({
        title: '登录失败',
        content: '网络错误，请重试',
        showCancel: false
      })
    }
  }
})
