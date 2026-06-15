const app = getApp()

Page({
  data: {
    nickname: '加载中...',
    userId: '---'
  },

  onLoad: function () {
    // 读取本地缓存的登录信息
    const userId = wx.getStorageSync('music_userId');
    const nickname = wx.getStorageSync('user_nickname'); 
    
    this.setData({
      userId: userId || '---',
      nickname: nickname || '网易云用户'
    });
  },

  // 更新 Cookies 处理逻辑
  handleUpdateCookies: function () {
    wx.showModal({
      title: '更新授权',
      content: '是否前往登录页重新扫码或账号登录以更新 Cookies？',
      confirmText: '确定',
      cancelText: '取消',
      confirmColor: '#00bcd4',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({
            url: '/pages/login/login'
          });
        }
      }
    });
  },

  // 退出登录处理逻辑
  handleLogout: function () {
    wx.showModal({
      title: '提示',
      content: '确定要退出当前账号吗？',
      confirmText: '确定',
      cancelText: '取消',
      confirmColor: '#ff4444',
      success: (res) => {
        if (res.confirm) {
          // 清理主页 isLoggedIn 依赖的本地鉴权缓存
          wx.removeStorageSync('music_userId');
          wx.removeStorageSync('user_nickname');
          wx.removeStorageSync('netease_cookie');
          wx.removeStorageSync('netease_cookie_timestamp');
          wx.removeStorageSync('genre_cache');
          
          wx.showToast({
            title: '已退出登录',
            icon: 'success',
            duration: 1000
          });

          // 重启小程序状态机，返回首页并销毁之前页面的音视频及轮询上下文
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/index/index'
            });
          }, 1000);
        }
      }
    });
  }
});