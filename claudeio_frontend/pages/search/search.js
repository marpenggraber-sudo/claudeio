const app = getApp()

Page({
  data: {
    keyword: '',
    songs: [],
    loading: false,
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    const userId = wx.getStorageSync('music_userId')
    if (!userId) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.apiBase}/search`,
      data: { keywords: this.data.keyword, userId: Number(userId) },
      success: res => this.setData({ songs: res.data?.songs || [] }),
      complete: () => this.setData({ loading: false }),
    })
  },
})
