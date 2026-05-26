// pages/logs/logs.js
const app = getApp()

Page({
  data: {
    userId: '',
    message: '',
    reply: '',
    songs: [],
    loading: false,
  },

  onLoad() {
    this.setData({ userId: wx.getStorageSync('music_userId') || '' })
  },

  onInput(e) {
    this.setData({ message: e.detail.value })
  },

  sendMessage() {
    const { message, userId } = this.data
    if (!message.trim()) return

    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.apiBase}/chat`,
      method: 'POST',
      data: { message, userId: userId ? Number(userId) : null },
      success: res => {
        this.setData({
          reply: res.data?.reply || '',
          songs: res.data?.songs || [],
          loading: false,
        })
      },
      fail: () => this.setData({ loading: false }),
    })
  },
})
