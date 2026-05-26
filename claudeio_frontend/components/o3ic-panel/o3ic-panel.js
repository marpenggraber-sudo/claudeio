// components/o3ic-panel/o3ic-panel.js

Component({
  properties: {
    // 解析后的歌词行数组 [{time: number, text: string}]
    o3icLines: {
      type: Array,
      value: [],
    },
    // 当前播放时间（秒）
    currentTime: {
      type: Number,
      value: 0,
    },
    // 是否全屏模式
    fullscreen: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    currentLineIndex: -1,
  },

  observers: {
    'o3icLines, currentTime': function (lines, time) {
      const idx = this._findCurrentLineIndex(lines, time)
      this.setData({ currentLineIndex: idx })
    },
  },

  methods: {
    // 二分查找当前行
    _findCurrentLineIndex(lines, time) {
      if (!lines || lines.length === 0) return -1
      let left = 0
      let right = lines.length - 1
      let result = -1
      while (left <= right) {
        const mid = (left + right) >> 1
        if (lines[mid].time <= time) {
          result = mid
          left = mid + 1
        } else {
          right = mid - 1
        }
      }
      return result
    },

    onTapLine(e) {
      const { time } = e.currentTarget.dataset
      this.triggerEvent('seek', { time })
    },
  },
})
