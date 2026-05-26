/**
 * utils/player.js
 * 音频播放器控制模块
 *
 * 使用方式：
 *   const player = require('./player.js')
 *   // 在 app.js 的 onLaunch 中：
 *   player.initPlayer(getApp())
 *   // 在页面中：
 *   player.playSong(song)
 */

let app = null
let audioCtx = null
let timeUpdateCallbacks = []
let endedCallbacks = []
let playUpdateCallbacks = []

/**
 * 初始化播放器（仅调用一次）
 * @param {Object} appInstance - getApp() 返回的实例
 */
function initPlayer(appInstance) {
  app = appInstance
  audioCtx = wx.createInnerAudioContext()

  // 设置音频
  audioCtx.volume = app.globalData.volume

  // 播放进度更新
  audioCtx.onTimeUpdate(() => {
    app.globalData.currentTime = audioCtx.currentTime
    app.globalData.duration = audioCtx.duration || 0
    timeUpdateCallbacks.forEach(cb => cb(audioCtx.currentTime, audioCtx.duration))
  })

  // 播放结束
  audioCtx.onEnded(() => {
    app.globalData.isPlaying = false
    notifyPlayUpdate()
    handleEnded()
  })

  // 播放错误
  audioCtx.onError(err => {
    console.error('[player] 播放错误:', err)
    app.globalData.isPlaying = false
    notifyPlayUpdate()
    // 自动跳下一首
    handleEnded()
  })

  // 音频加载中
  audioCtx.onWaiting(() => {
    // 可触发 loading 状态
  })

  // 音频可以播放
  audioCtx.onCanplay(() => {
    // 可以开始播放
  })

  app.globalData.audioContext = audioCtx
}

/**
 * 播放指定歌曲
 * @param {Object} song - 歌曲对象（需包含 id, name, artists, album 等）
 * @returns {Promise<void>}
 */
async function playSong(song) {
  const netease = require('./netease.js')

  app.globalData.currentSong = song
  app.globalData.isPlaying = true
  notifyPlayUpdate()

  try {
    // 获取用户ID以使用认证cookie
    const userId = wx.getStorageSync('music_userId')
    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      app.globalData.isPlaying = false
      notifyPlayUpdate()
      return
    }

    const url = await netease.getSongUrl(song.id, userId)
    if (!url) {
      wx.showToast({ title: '该歌曲无播放版权', icon: 'none' })
      app.globalData.isPlaying = false
      notifyPlayUpdate()
      handleEnded()
      return
    }

    audioCtx.src = url
    audioCtx.title = song.name
    audioCtx.epname = song.album?.name || ''
    audioCtx.singer = (song.artists || []).map(a => a.name).join(', ')

    audioCtx.play()

    // 更新全局时长
    if (song.duration) {
      app.globalData.duration = song.duration / 1000  // 毫秒转秒
    }

  } catch (err) {
    console.error('[player] playSong error:', err)
    wx.showToast({ title: '播放失败，请重试', icon: 'none' })
    app.globalData.isPlaying = false
    notifyPlayUpdate()
  }
}

/**
 * 播放播放队列中的歌曲
 * @param {Object} song - 歌曲对象
 * @param {Array} queue - 播放队列
 * @param {number} index - 歌曲在队列中的索引
 */
function playQueueSong(song, queue, index) {
  app.globalData.playQueue = queue
  app.globalData.queueIndex = index
  playSong(song)
}

/**
 * 暂停播放
 */
function pause() {
  if (audioCtx) {
    audioCtx.pause()
    app.globalData.isPlaying = false
    notifyPlayUpdate()
  }
}

/**
 * 继续播放
 */
function resume() {
  if (audioCtx && audioCtx.src) {
    audioCtx.play()
    app.globalData.isPlaying = true
    notifyPlayUpdate()
  }
}

/**
 * 切换播放/暂停
 */
function togglePlay() {
  if (app.globalData.isPlaying) {
    pause()
  } else {
    resume()
  }
}

/**
 * 跳转到指定时间
 * @param {number} time - 时间（秒）
 */
function seek(time) {
  if (audioCtx) {
    audioCtx.seek(time)
    app.globalData.currentTime = time
    notifyPlayUpdate()
  }
}

/**
 * 设置音量
 * @param {number} v - 音量 0~1
 */
function setVolume(v) {
  if (audioCtx) {
    audioCtx.volume = v
    app.globalData.volume = v
  }
}

/**
 * 下一首
 */
function skipToNext() {
  const { playQueue, queueIndex, playMode } = app.globalData
  if (!playQueue.length) return

  let newIndex
  if (playMode === 'random') {
    newIndex = Math.floor(Math.random() * playQueue.length)
  } else {
    newIndex = (queueIndex + 1) % playQueue.length
  }

  app.globalData.queueIndex = newIndex
  playSong(playQueue[newIndex])
}

/**
 * 上一首
 */
function skipToPrev() {
  const { playQueue, queueIndex } = app.globalData
  if (!playQueue.length) return

  // 如果当前播放超过 3 秒，则重新播放；否则跳上一首
  if (app.globalData.currentTime > 3) {
    seek(0)
    return
  }

  const newIndex = (queueIndex - 1 + playQueue.length) % playQueue.length
  app.globalData.queueIndex = newIndex
  playSong(playQueue[newIndex])
}

/**
 * 处理播放结束后的行为（根据播放模式）
 */
function handleEnded() {
  const { playMode } = app.globalData
  if (playMode === 'single') {
    seek(0)
    resume()
  } else {
    skipToNext()
  }
}

/**
 * 订阅播放进度更新
 * @param {Function} callback (currentTime, duration) => void
 */
function onTimeUpdate(callback) {
  timeUpdateCallbacks.push(callback)
  // 返回取消函数
  return () => {
    timeUpdateCallbacks = timeUpdateCallbacks.filter(cb => cb !== callback)
  }
}

/**
 * 订阅播放状态变化（播放/暂停/切歌）
 * @param {Function} callback () => void
 */
function onPlayUpdate(callback) {
  playUpdateCallbacks.push(callback)
  return () => {
    playUpdateCallbacks = playUpdateCallbacks.filter(cb => cb !== callback)
  }
}

/**
 * 通知所有播放状态观察者
 */
function notifyPlayUpdate() {
  playUpdateCallbacks.forEach(cb => cb())
}

/**
 * 设置播放模式
 * @param {'list'|'single'|'random'} mode
 */
function setPlayMode(mode) {
  app.globalData.playMode = mode
  wx.setStorageSync('playMode', mode)
}

/**
 * 从缓存恢复播放模式
 */
function restorePlayMode() {
  const saved = wx.getStorageSync('playMode')
  if (['list', 'single', 'random'].includes(saved)) {
    app.globalData.playMode = saved
  }
}

module.exports = {
  initPlayer,
  playSong,
  playQueueSong,
  pause,
  resume,
  togglePlay,
  seek,
  setVolume,
  skipToNext,
  skipToPrev,
  onTimeUpdate,
  onPlayUpdate,
  setPlayMode,
  restorePlayMode,
}
