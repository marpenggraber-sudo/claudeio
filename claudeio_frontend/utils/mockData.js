/**
 * utils/mockData.js
 * 开发阶段 Mock 数据，替换 USE_MOCK = false 时使用真实 API
 */

const MOCK_SONGS = [
  {
    id: 190137,
    name: '晴天',
    artists: [{ id: 6452, name: '周杰伦' }],
    album: { id: 18809, name: '叶惠美', picUrl: 'https://p2.music.126.net/4hD2RnhwMjnfPwC7Nz3eLQ==/109951166575206912.jpg' },
    duration: 269000,
    fee: 0,
  },
  {
    id: 188469,
    name: '七里香',
    artists: [{ id: 6452, name: '周杰伦' }],
    album: { id: 18865, name: '七里香', picUrl: 'https://p1.music.126.net/ztGrCq_P5E0vldsNkvkqTeg==/109951166575198716.jpg' },
    duration: 299000,
    fee: 0,
  },
  {
    id: 186453,
    name: '稻香',
    artists: [{ id: 6452, name: '周杰伦' }],
    album: { id: 17692, name: '魔杰座', picUrl: 'https://p2.music.126.net/wxzU7RO7FMKeUnEI4I599g==/109951166575185847.jpg' },
    duration: 223000,
    fee: 0,
  },
  {
    id: 505248,
    name: '夜曲',
    artists: [{ id: 6452, name: '周杰伦' }],
    album: { id: 48, name: '十一月的萧邦', picUrl: 'https://p1.music.126.net/6y-Uslvjf7n/b5uC2e5n9K9K1L3==/109951166575166401.jpg' },
    duration: 257000,
    fee: 0,
  },
  {
    id: 185906,
    name: '蒲公英的约定',
    artists: [{ id: 6452, name: '周杰伦' }],
    album: { id: 17692, name: '魔杰座', picUrl: 'https://p2.music.126.net/wxzU7RO7fmKeUnEI4I599g==/109951166575185847.jpg' },
    duration: 262000,
    fee: 0,
  },
  {
    id: 27808148,
    name: '起风了',
    artists: [{ id: 12138269, name: '买辣椒也用券' }],
    album: { id: 3659078, name: '起风了', picUrl: 'https://p2.music.126.net/d84x9sP8TANiV8I-1irlwg==/109951166587252928.jpg' },
    duration: 298000,
    fee: 0,
  },
  {
    id: 440239,
    name: 'Hotel California',
    artists: [{ id: 9606, name: 'Eagles' }],
    album: { id: 38527, name: 'Their Greatest Hits 1971-1975', picUrl: 'https://p1.music.126.net/WHskVYEvW2Pt1RoI_9_C8g==/109951166575178432.jpg' },
    duration: 391000,
    fee: 0,
  },
  {
    id: 22648361,
    name: 'Shape of You',
    artists: [{ id: 983382, name: 'Ed Sheeran' }],
    album: { id: 35792923, name: '÷ (Divide)', picUrl: 'https://p1.music.单曲/126.net/4eHcdT8V7tHN1V8H8P9N8g==/109951166590123456.jpg' },
    duration: 234000,
    fee: 0,
  },
]

// Mock 歌词（晴天 by 周杰伦）
const MOCK_LYRIC_QINGTIAN = `[00:00.00]作曲 : 周杰伦
[00:08.00]作词 : 方文山
[00:16.00]编曲 : 周杰伦
[00:24.00]
[00:32.00]G         Em
[00:32.50]故事的小黄花
[00:36.00]从出生那年就飘着
[00:39.50]Bm              Am
[00:40.00]童年的荡秋千
[00:43.50]随记忆一直晃到现在
[00:47.00]G         Em
[00:47.50]吹着前奏望着天空
[00:51.00]Bm              Am
[00:51.50]想起花瓣试着掉落
[00:54.50]G    Em
[00:55.00]等你下课  上课  下课
[01:00.00]
[01:08.00]Am              G
[01:08.50]靠着墙壁  背我的阳光
[01:12.00]F          Em
[01:12.50]想笑  阳光  闪躲
[01:16.00]Am              G
[01:16.50]这校园  跟 青春一样
[01:20.00]F      G
[01:20.50]总要有诗
[01:23.50]
[01:28.00]G         Em
[01:28.50]为学园  的旋律
[01:32.00]Bm              Am
[01:32.50]在这童年的故事里
[01:36.00]G    Em
[01:36.50]存在着 我和你
[01:40.00]F          G
[01:40.50]那童年的纯真
[01:44.00]Am              G
[01:44.50]一起走过的时光
[01:48.00]F          G
[01:48.50]一起许下的愿望
[01:52.00]Am      G
[01:52.50]一起成长
[01:56.00]
[02:00.00]G         Em
[02:00.50]童年的纯真
[02:04.00]Bm              Am
[02:04.50]一起走过的时光
[02:08.00]G    Em
[02:08.50]一起许下的愿望
[02:12.00]F      G
[02:12.50]一起成长
[02:16.00]
[02:20.00]Am              G
[02:20.50]总有一天  我会 长大
[02:24.00]F          Em
[02:24.50]带着 梦想  出发
[02:28.00]Am              G
[02:28.50]总有一天  你会 听见
[02:32.00]F      G
[02:32.50]那青春的 回答
[02:36.00]
[02:44.00]G         Em
[02:44.50]故事的小黄花
[02:48.00]从出生那年就飘着
[02:51.50]Bm              Am
[02:52.00]童年的荡秋千
[02:55.50]随记忆一直晃到现在
[02:59.00]G         Em
[02:59.50]吹着前奏望着天空
[03:03.00]Bm              Am
[03:03.50]想起花瓣试着掉落
[03:07.00]G
[03:07.50]为了你  而祈祷
[03:11.00]F      G
[03:11.50]而祝福
[03:15.00]
[03:20.00]而牵挂
[03:24.00]G
[03:24.50]是我坚持的 理由
[03:28.00]F          G
[03:28.50]只为你  而存在
[03:32.00]Am      G
[03:32.50]只为你
[03:36.00]
[03:40.00]G
[03:40.50]只为你
[03:44.00]
[03:48.00]（End）`

// Mock 播放 URL（使用公开的示例音频 URL，实际播放时可能需要真实版权 URL）
const MOCK_AUDIO_URL = 'https://music.163.com/song/media/outer/url?id=190137.mp3'

const MOCK_PLAYLISTS = [
  {
    id: 1001,
    name: '我喜欢的音乐',
    coverImgUrl: 'https://p2.music.126.net/4hD2RnhwMjnfPwC7Nz3eLQ==/109951166575206912.jpg',
    trackCount: 42,
    creator: { nickname: '用户' },
    tracks: MOCK_SONGS,
  },
  {
    id: 1002,
    name: '工作 BGM',
    coverImgUrl: 'https://p1.music.126.net/H4P1Tj1NmP5e7nP2H1H5Rw==/109951166587252928.jpg',
    trackCount: 15,
    creator: { nickname: '用户' },
    tracks: [MOCK_SONGS[5], MOCK_SONGS[6]],
  },
]

// Mock 搜索结果（根据关键词返回）
function getMockSearchResults(keywords) {
  const kw = keywords.toLowerCase()
  if (kw.includes('周杰伦') || kw.includes('jay')) {
    return MOCK_SONGS.filter(s => s.artists.some(a => a.name.includes('周杰伦')))
  }
  if (kw.includes('英文') || kw.includes('english')) {
    return MOCK_SONGS.filter(s => s.artists.some(a => !a.name.includes('周')))
  }
  if (kw.includes('欢快') || kw.includes('开心') || kw.includes('happy')) {
    return [MOCK_SONGS[1], MOCK_SONGS[2], MOCK_SONGS[5]]
  }
  if (kw.includes('抒情') || kw.includes('慢') || kw.includes('安静')) {
    return [MOCK_SONGS[0], MOCK_SONGS[3], MOCK_SONGS[4]]
  }
  return MOCK_SONGS
}

// Mock 推荐歌曲（根据心情/场景）
function getMockRecommendByMood(mood, scene) {
  if (mood === 'happy' || mood === '欢快' || mood === '开心') {
    return [MOCK_SONGS[1], MOCK_SONGS[2], MOCK_SONGS[5]]
  }
  if (mood === 'sad' || mood === '悲伤') {
    return [MOCK_SONGS[0], MOCK_SONGS[3], MOCK_SONGS[4]]
  }
  if (scene === 'working' || scene === '工作') {
    return [MOCK_SONGS[5], MOCK_SONGS[6]]
  }
  return MOCK_SONGS.slice(0, 3)
}

module.exports = {
  MOCK_SONGS,
  MOCK_PLAYLISTS,
  MOCK_LYRIC_QINGTIAN,
  MOCK_AUDIO_URL,
  getMockSearchResults,
  getMockRecommendByMood,
}
