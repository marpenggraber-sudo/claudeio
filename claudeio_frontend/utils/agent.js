/**
 * utils/agent.js
 * AI 意图识别与对话执行引擎
 *
 * 纯本地实现，无需后端。
 * parseIntent() 解析用户输入的意图，
 * executeIntent() 根据意图调用 netease.js 执行操作。
 */

const netease = require('./netease.js')
const { generateId } = require('./format.js')

// ====================  意图类型常量  ====================

const Intent = {
  PLAY_SONG: 'PLAY_SONG',           // 播放指定歌曲
  PLAY_BY_MOOD: 'PLAY_BY_MOOD',     // 按心情播放
  PLAY_BY_SCENE: 'PLAY_BY_SCENE',   // 按场景播放
  PLAY_BY_ARTIST: 'PLAY_BY_ARTIST', // 按歌手播放
  PLAY_RECOMMEND: 'PLAY_RECOMMEND', // 系统推荐
  PLAY_PLAYLIST: 'PLAY_PLAYLIST',   // 播放歌单
  PLAY_RANDOM: 'PLAY_RANDOM',       // 随机播放
  CONTROL: 'CONTROL',               // 播放控制（暂停/下一首等）
  INFO_QUERY: 'INFO_QUERY',         // 信息查询
  SEARCH: 'SEARCH',                 // 搜索歌曲
  UNKNOWN: 'UNKNOWN',               // 未知意图
}

// ====================  快捷命令映射  ====================

const SLASH_COMMANDS = {
  '/play': { intent: Intent.CONTROL, action: 'play' },
  '/pause': { intent: Intent.CONTROL, action: 'pause' },
  '/next': { intent: Intent.CONTROL, action: 'next' },
  '/prev': { intent: Intent.CONTROL, action: 'prev' },
  '/random': { intent: Intent.PLAY_RANDOM, action: 'random' },
  '/loop': { intent: Intent.CONTROL, action: 'loop' },
  '/shuffle': { intent: Intent.CONTROL, action: 'shuffle' },
  '/help': { intent: Intent.UNKNOWN, action: 'help' },
}

// ====================  关键词词典  ====================

const MOOD_KEYWORDS = {
  happy: ['开心', '欢快', '快乐', '高兴', '愉快', 'happy', 'upbeat', ' cheerful'],
  sad: ['悲伤', '难过', '伤心', '忧郁', 'sad', 'blue', 'melancholy'],
  calm: ['平静', '安静', '舒缓', '放松', '轻柔', 'calm', 'peaceful', 'relax'],
  energetic: ['动感', '活力', '激情', '热烈', 'energetic', 'intense'],
  romantic: ['浪漫', '甜蜜', '温馨', '浪漫', 'romantic', 'love'],
  dark: ['暗黑', '阴郁', '神秘', 'dark', 'mysterious'],
}

const SCENE_KEYWORDS = {
  working: ['工作', '办公', '学习', 'concentrate', 'focus', 'study', 'work'],
  sleeping: ['睡觉', '睡前', '助眠', '睡眠', 'sleep', 'night'],
  morning: ['早上', '早晨', '起床', 'morning', 'wake'],
  driving: ['开车', '驾驶', '旅途', 'drive', 'road'],
  exercise: ['运动', '跑步', '健身', 'exercise', 'running', 'gym'],
  dating: ['约会', '浪漫', 'dating', 'romantic'],
  party: ['派对', '聚会', 'party'],
}

const CONTROL_KEYWORDS = {
  pause: ['暂停', '停止播放', 'pause'],
  resume: ['继续', '恢复播放', 'resume', '继续播放'],
  next: ['下一首', '切到下一首', 'next', 'skip'],
  prev: ['上一首', '前一首', '上一曲', 'prev', 'previous'],
  loop: ['单曲循环', '循环', 'loop', 'repeat'],
  shuffle: ['随机播放', '打乱', 'shuffle', 'random'],
  volume_up: ['调大音量', '声音大点', ' louder'],
  volume_down: ['调小音量', '声音小点', 'quieter'],
}

// ====================  核心函数  ====================

/**
 * 解析用户输入，返回意图对象
 * @param {string} input - 用户原始输入
 * @returns {{ intent: string, params: Object, rawInput: string }}
 */
function parseIntent(input) {
  if (!input || typeof input !== 'string') {
    return { intent: Intent.UNKNOWN, params: {}, rawInput: input }
  }

  const text = input.trim()

  // 1. 检查快捷命令（/xxx）
  if (text.startsWith('/')) {
    const cmd = text.split(' ')[0].toLowerCase()
    if (SLASH_COMMANDS[cmd]) {
      return {
        intent: SLASH_COMMANDS[cmd].intent,
        params: { action: SLASH_COMMANDS[cmd].action, full: text },
        rawInput: text,
      }
    }
    // /play xxx 格式
    if (cmd === '/play' && text.length > 5) {
      return { intent: Intent.PLAY_SONG, params: { keywords: text.slice(5).trim() }, rawInput: text }
    }
    return { intent: Intent.UNKNOWN, params: { action: 'unknown_command' }, rawInput: text }
  }

  // 2. 检查播放控制指令
  for (const [action, kws] of Object.entries(CONTROL_KEYWORDS)) {
    if (kws.some(kw => text.includes(kw))) {
      return { intent: Intent.CONTROL, params: { action }, rawInput: text }
    }
  }

  // 3. 提取歌手名（简单的启发式）
  const artistMatch = text.match(/^(.+?)\s*(的|的歌|所有)$/)
  if (artistMatch && text.length < 20) {
    return {
      intent: Intent.PLAY_BY_ARTIST,
      params: { artist: artistMatch[1].trim() },
      rawInput: text,
    }
  }

  // 4. 检查心情关键词
  for (const [mood, kws] of Object.entries(MOOD_KEYWORDS)) {
    if (kws.some(kw => text.includes(kw))) {
      return {
        intent: Intent.PLAY_BY_MOOD,
        params: { mood, rawText: text },
        rawInput: text,
      }
    }
  }

  // 5. 检查场景关键词
  for (const [scene, kws] of Object.entries(SCENE_KEYWORDS)) {
    if (kws.some(kw => text.includes(kw))) {
      return {
        intent: Intent.PLAY_BY_SCENE,
        params: { scene, rawText: text },
        rawInput: text,
      }
    }
  }

  // 6. 检查推荐意图
  const recommendKws = ['推荐', '随便放', '随便听', '给我来点', 'recommend', 'suggest']
  if (recommendKws.some(kw => text.includes(kw))) {
    return { intent: Intent.PLAY_RECOMMEND, params: { rawText: text }, rawInput: text }
  }

  // 7. 检查歌单意图
  const playlistKws = ['歌单', '播放列表', 'playlist', '收藏', '收藏夹']
  if (playlistKws.some(kw => text.includes(kw))) {
    return { intent: Intent.PLAY_PLAYLIST, params: { rawText: text }, rawInput: text }
  }

  // 8. 默认：当作搜索歌曲处理
  if (text.length > 0) {
    return {
      intent: Intent.PLAY_SONG,
      params: { keywords: text },
      rawInput: text,
    }
  }

  return { intent: Intent.UNKNOWN, params: {}, rawInput: text }
}

/**
 * 根据意图生成 AI 回复文本
 * @param {string} intent
 * @param {Object} params
 * @returns {string}
 */
function generateAIResponse(intent, params) {
  switch (intent) {
    case Intent.PLAY_SONG:
      return `正在搜索：「${params.keywords}」...`
    case Intent.PLAY_BY_MOOD:
      return `正在为你挑选${params.mood}风格的歌曲...`
    case Intent.PLAY_BY_SCENE:
      return `为你准备了适合${params.scene}场景的音乐...`
    case Intent.PLAY_BY_ARTIST:
      return `正在加载 ${params.artist} 的歌曲...`
    case Intent.PLAY_RECOMMEND:
      return '根据你的喜好，为你推荐以下歌曲：'
    case Intent.PLAY_PLAYLIST:
      return '正在加载你的歌单...'
    case Intent.PLAY_RANDOM:
      return '正在随机挑选一首歌曲...'
    case Intent.CONTROL:
      return `已执行：${params.action}`
    default:
      return '好的，正在处理...'
  }
}

/**
 * 执行意图，返回执行结果
 * @param {{ intent, params }} parsed - parseIntent 返回的意图对象
 * @param {Object} appGlobalData - getApp().globalData
 * @returns {Promise<{ type: string, songs?: Array, message?: string }>}
 */
async function executeIntent(parsed, appGlobalData) {
  const { intent, params } = parsed

  switch (intent) {
    case Intent.PLAY_SONG: {
      const songs = await netease.searchSongs(params.keywords)
      if (!songs.length) {
        return { type: 'error', message: `没有找到与「${params.keywords}」相关的歌曲，换个关键词试试？` }
      }
      // 只取前 3 首
      return { type: 'songs', songs: songs.slice(0, 3), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.PLAY_BY_MOOD: {
      const songs = await netease.getRecommendByMood(params.mood, null)
      if (!songs.length) {
        return { type: 'error', message: '暂时没有找到符合这个心情的歌曲，换个心情试试？' }
      }
      return { type: 'songs', songs: songs.slice(0, 3), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.PLAY_BY_SCENE: {
      const songs = await netease.getRecommendByMood(null, params.scene)
      if (!songs.length) {
        return { type: 'error', message: '暂时没有找到适合这个场景的歌曲。' }
      }
      return { type: 'songs', songs: songs.slice(0, 3), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.PLAY_BY_ARTIST: {
      const songs = await netease.searchSongs(params.artist)
      if (!songs.length) {
        return { type: 'error', message: `没有找到歌手「${params.artist}」的歌曲。` }
      }
      return { type: 'songs', songs: songs.slice(0, 5), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.PLAY_RECOMMEND: {
      const songs = await netease.getRecommendByMood(null, null)
      return { type: 'songs', songs: songs.slice(0, 3), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.PLAY_RANDOM: {
      // 随机播放一首
      const songs = await netease.searchSongs('华语流行')
      const shuffled = songs.sort(() => Math.random() - 0.5)
      return { type: 'songs', songs: shuffled.slice(0, 1), aiMessage: generateAIResponse(intent, params) }
    }

    case Intent.CONTROL: {
      const player = require('./player.js')
      switch (params.action) {
        case 'pause':
          player.pause()
          return { type: 'control', action: 'paused', aiMessage: '已暂停' }
        case 'play':
          player.resume()
          return { type: 'control', action: 'playing', aiMessage: '继续播放' }
        case 'next':
          player.skipToNext()
          return { type: 'control', action: 'next', aiMessage: '正在播放下一首...' }
        case 'prev':
          player.skipToPrev()
          return { type: 'control', action: 'prev', aiMessage: '正在播放上一首...' }
        case 'loop':
          player.setPlayMode('single')
          return { type: 'control', action: 'loop', aiMessage: '已开启单曲循环' }
        case 'shuffle':
          player.setPlayMode('random')
          return { type: 'control', action: 'shuffle', aiMessage: '已开启随机播放' }
        default:
          return { type: 'control', action: 'done', aiMessage: '已执行' }
      }
    }

    default:
      return {
        type: 'error',
        message: '我没能理解你的意思，请试试更具体的描述，例如："播放周杰伦的歌" 或 "来点欢快的音乐"',
      }
  }
}

/**
 * 构建对话消息列表（用于首页展示）
 * @param {Array} messages - 当前消息列表
 * @param {string} userInput - 用户输入
 * @param {{ type, songs?, message?, aiMessage? }} result - executeIntent 返回结果
 * @returns {Array} 更新后的消息列表
 */
function buildMessages(messages, userInput, result) {
  const newMessages = [...messages]

  // 添加用户消息
  newMessages.push({
    id: generateId(),
    type: 'user',
    content: userInput,
    timestamp: Date.now(),
  })

  // 添加 AI 思考中消息
  const aiThinkingId = generateId()
  newMessages.push({
    id: aiThinkingId,
    type: 'ai',
    content: result.aiMessage || generateAIResponse(null, {}),
    timestamp: Date.now(),
  })

  // 添加歌曲结果或错误消息
  if (result.type === 'songs' && result.songs) {
    result.songs.forEach(song => {
      newMessages.push({
        id: generateId(),
        type: 'result',
        song,
        timestamp: Date.now(),
      })
    })
  } else if (result.type === 'error') {
    newMessages.push({
      id: generateId(),
      type: 'system',
      content: result.message,
      timestamp: Date.now(),
    })
  }

  return newMessages
}

module.exports = {
  Intent,
  SLASH_COMMANDS,
  parseIntent,
  executeIntent,
  generateAIResponse,
  buildMessages,
}
