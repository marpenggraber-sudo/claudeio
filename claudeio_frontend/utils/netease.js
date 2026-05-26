/**
 * utils/netease.js
 * NeteaseCloudMusicApi 封装层
 *
 * 使用说明：
 *   1. 开发阶段：USE_MOCK = true（默认），使用内置 mock 数据
 *   2. 部署阶段：USE_MOCK = false，将 BASE_URL 替换为你的代理地址
 *
 * 微信小程序要求：
 *   - 所有请求必须走 HTTPS
 *   - 域名必须在 MP 后台 → 开发管理 → 开发设置 → 服务器域名 中添加 request 合法域名
 *   - 建议架设一层 Node.js 代理（中转请求 + 注入 Cookie）
 */

// =====================  配置区  =====================
const USE_MOCK = false  // 开发阶段设为 true；部署时改为 false
const BASE_URL = 'http://localhost:8080/api/music'  // 真实代理地址，部署时填写
// ====================  配置区结束  ====================

const mockData = require('./mockData.js')

// ====================  辅助函数  ====================

/**
 * 统一请求封装
 */
function request(url, data = {}, method = 'GET') {
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      data,
      method,
      header: {
        'Content-Type': 'application/json',
        // Cookie: 代理层统一注入，无需在此处手动添加
      },
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(`HTTP ${res.statusCode}: ${JSON.stringify(res.data)}`))
        }
      },
      fail: err => {
        reject(new Error(`请求失败: ${err.errMsg}`))
      },
    })
  })
}

// ====================  API 方法  ====================

/**
 * 搜索歌曲
 * @param {string} keywords - 搜索关键词
 * @returns {Promise<Array>} 歌曲列表
 */
async function searchSongs(keywords) {
  if (USE_MOCK) {
    // 模拟网络延迟
    await delay(300 + Math.random() * 400)
    return mockData.getMockSearchResults(keywords)
  }

  const res = await request(`${BASE_URL}/search?keywords=${encodeURIComponent(keywords)}&limit=20`)
  return res.result?.songs || []
}

/**
 * 获取歌曲播放 URL
 * @param {number} songId
 * @param {number} userId - 用户ID，用于获取认证cookie
 * @returns {Promise<string|null>} 音频 URL，无版权返回 null
 */
async function getSongUrl(songId, userId) {
  if (USE_MOCK) {
    await delay(200)
    // Mock URL：直接返回网易云的公开示例 URL
    return mockData.MOCK_AUDIO_URL
  }

  const res = await request(`${BASE_URL}/play-url?songId=${songId}&userId=${userId}`)
  const url = res.url
  if (!url) {
    console.warn(`[netease] 歌曲 ${songId} 无播放版权`)
    return null
  }
  return url
}

/**
 * 获取歌曲详情（封面、专辑、时长等）
 * @param {number|Array<number>} ids - 歌曲 ID 或 ID 数组
 * @returns {Promise<Object|Array>}
 */
async function getSongDetail(ids) {
  const idList = Array.isArray(ids) ? ids.join(',') : ids

  if (USE_MOCK) {
    await delay(150)
    const songs = mockData.MOCK_SONGS.filter(s => s.id == idList || String(s.id) === String(idList))
    if (songs.length === 1) return songs[0]
    if (songs.length > 1) return songs
    return mockData.MOCK_SONGS[0]
  }

  const res = await request(`${BASE_URL}/song/detail?ids=${idList}`)
  return res.songs || res
}

/**
 * 获取歌词
 * @param {number} songId
 * @returns {Promise<string>} LRC 格式歌词
 */
async function getLyric(songId) {
  if (USE_MOCK) {
    await delay(200)
    // 目前 mock 只内置了晴天的歌词，其他返回空
    if (songId === 190137) return mockData.MOCK_LYRIC_QINGTIAN
    return '[00:00.00]该歌曲暂无歌词'
  }

  const res = await request(`${BASE_URL}/o3ic?id=${songId}`)
  return res.lrc?.o3ic || ''
}

/**
 * 获取用户歌单列表
 * @param {number} uid - 用户 ID
 * @returns {Promise<Array>} 歌单列表
 */
async function getUserPlaylist(uid) {
  if (USE_MOCK) {
    await delay(300)
    return mockData.MOCK_PLAYLISTS
  }

  const res = await request(`${BASE_URL}/user/playlist?uid=${uid}`)
  return res.playlist || []
}

/**
 * 获取歌单详情
 * @param {number} playlistId
 * @returns {Promise<Object>}
 */
async function getPlaylistDetail(playlistId) {
  if (USE_MOCK) {
    await delay(300)
    const playlist = mockData.MOCK_PLAYLISTS.find(p => p.id === playlistId)
    return playlist || mockData.MOCK_PLAYLISTS[0]
  }

  const res = await request(`${BASE_URL}/playlist/detail?id=${playlistId}`)
  return res.result || res
}

/**
 * 获取根据心情/场景推荐的歌曲
 * @param {string} mood - 心情
 * @param {string} scene - 场景
 * @returns {Promise<Array>}
 */
async function getRecommendByMood(mood, scene) {
  if (USE_MOCK) {
    await delay(400)
    return mockData.getMockRecommendByMood(mood, scene)
  }

  // 真实场景：使用网易云 "每日推荐" 或 tag 歌单
  // 这里简化为搜索 fallback
  const kw = [mood, scene].filter(Boolean).join(' ')
  return searchSongs(kw)
}

// ====================  LRC 歌词解析  ====================

/**
 * 解析 LRC 格式歌词为时间轴数组
 * @param {string} lrcText - LRC 歌词文本
 * @returns {Array<{time: number, text: string}>}
 */
function parseLyric(lrcText) {
  if (!lrcText) return []
  const lines = lrcText.split('\n')
  const result = []
  lines.forEach(line => {
    const match = line.match(/\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)/)
    if (match) {
      const min = parseInt(match[1])
      const sec = parseInt(match[2])
      const ms = match[3] ? parseInt(match[3].padEnd(3, '0').slice(0, 3)) : 0
      const text = match[4].trim()
      if (text) {
        result.push({ time: min * 60 + sec + ms / 1000, text })
      }
    }
  })
  return result
}

// ====================  辅助函数  ====================

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 将网易云歌曲数据格式化为统一 Song 对象
 * @param {Object} raw - 原始歌曲数据
 * @returns {Object}
 */
function normalizeSong(raw) {
  return {
    id: raw.id,
    name: raw.name || '未知歌曲',
    artists: (raw.artists || raw.ar || []).map(a => ({ id: a.id, name: a.name })),
    album: raw.album || raw.al || { name: '未知专辑', picUrl: '' },
    duration: raw.duration || 0,
    fee: raw.fee || 0,  // 0=免费, 1=VIP, 4=版权限制
    picUrl: (raw.album || raw.al || {}).picUrl || (raw.album || {}).pic_str || '',
  }
}

module.exports = {
  USE_MOCK,
  searchSongs,
  getSongUrl,
  getSongDetail,
  getLyric,
  getUserPlaylist,
  getPlaylistDetail,
  getRecommendByMood,
  parseLyric,
  normalizeSong,
}
