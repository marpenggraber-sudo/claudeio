// components/song-card/song-card.js
const { formatDuration } = require('../../utils/format.js')

Component({
  properties: {
    song: {
      type: Object,
      value: null,
    },
    index: {
      type: Number,
      value: null,
    },
    isPlaying: {
      type: Boolean,
      value: false,
    },
    showIndex: {
      type: Boolean,
      value: false,
    },
    // 来自 app.globalData 的播放中歌曲 ID，用于自动判断是否在播放
    activeSongId: {
      type: Number,
      value: null,
    },
  },

  data: {
    durationStr: '00:00',
    artistName: '',
    isCurrentPlaying: false,
  },

  observers: {
    'song, isPlaying, activeSongId': function (song, isPlaying, activeSongId) {
      if (!song) return
      const durationSec = song.duration ? song.duration / 1000 : 0
      const artists = (song.artists || []).map(a => a.name).join(', ')
      const isCurrent = activeSongId ? song.id === activeSongId : isPlaying
      this.setData({
        durationStr: formatDuration(durationSec),
        artistName: artists || '未知艺术家',
        isCurrentPlaying: isCurrent,
      })
    },
  },

  methods: {
    onPlayTap() {
      this.triggerEvent('play', { song: this.data.song })
    },

    onMoreTap() {
      this.triggerEvent('more', { song: this.data.song })
    },
  },
})
