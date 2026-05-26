// components/dot-visualizer/dot-visualizer.js
Component({
  properties: {
    isPlaying: {
      type: Boolean,
      value: false,
    },
    themeBg: {
      type: String,
      value: '#0d0d0d',
    },
    themeText: {
      type: String,
      value: '#F9FAFB',
    },
  },

  data: {
    displayTime: '00:00:00',
    displayDay: 'Mon',
    displayDate: '2026/01/01',
    timeColor: '#F9FAFB',
    timeColorMeta: 'rgba(249,250,251,0.55)',
    timeGlowColor: 'rgba(255,255,255,0.6)',
  },

  _ctx: null,
  _canvasNode: null,
  _canvasW: 375,
  _canvasH: 375,
  _cols: 24,
  _rows: 24,
  _r: 5,
  _gapX: 14,
  _gapY: 14,
  _dotSizes: [],
  _animTimer: null,
  _clockTimer: null,
  _freq: [],
  _ready: false,
  _inited: false,

  lifetimes: {
    attached() {
      this._initClock()
      this._initCanvas()
    },

    detached() {
      this._cleanup()
    },
  },

  observers: {
    isPlaying(val) {
      if (val) this._startAnim()
      else this._stopAnim()
    },
    themeBg() {
      if (this._ready) this._drawStatic()
    },
  },

  methods: {
    noop() {},

    // ==================== 时钟 ====================

    _initClock() {
      this._updateClock()
      this._clockTimer = setInterval(() => this._updateClock(), 1000)
    },

    _updateClock() {
      const now = new Date()
      const pad = n => String(n).padStart(2, '0')
      this.setData({
        displayTime: `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`,
        displayDay: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][now.getDay()],
        displayDate: `${now.getFullYear()}/${pad(now.getMonth() + 1)}/${pad(now.getDate())}`,
      })
    },

    // ==================== Canvas 初始化（先算尺寸不依赖 DOM，后补 node）====================

    _initCanvas() {
      if (this._inited) return
      this._inited = true

      // 立即用新 API 算出尺寸，让 canvas 能立刻渲染
      const windowInfo = wx.getWindowInfo()
      const screenW = windowInfo.windowWidth
      this._canvasW = Math.round(screenW)
      this._canvasH = Math.round(screenW)

      this._cols = Math.floor(this._canvasW / (this._r * 2 + this._gapX))
      this._rows = Math.floor(this._canvasH / (this._r * 2 + this._gapY))

      this._dotSizes = []
      for (let row = 0; row < this._rows; row++) {
        this._dotSizes[row] = new Array(this._cols).fill(0)
      }

      this._ready = true
      this._drawStatic()

      // 再异步拿 canvas node 用于 requestAnimationFrame
      this._fetchCanvasNode()
    },

    _fetchCanvasNode() {
      const tryCount = this._fetchCount || 0
      this._fetchCount = tryCount + 1
      if (tryCount >= 20) return // 最多等 2 秒

      wx.nextTick(() => {
        const query = wx.createSelectorQuery().in(this)
        query.select('#dotCanvas').node().exec(([res]) => {
          if (!res || !res.node) {
            this._fetchCanvasNode()
            return
          }
          const canvasNode = res.node
          canvasNode.width = this._canvasW
          canvasNode.height = this._canvasH
          this._ctx = canvasNode.getContext('2d')
          this._canvasNode = canvasNode
          this._fetchCount = 0
        })
      })
    },

    // ==================== 静态背景（兼容新旧 Canvas API）====================

    _drawStatic() {
      if (!this._ready) return
      const isNewApi = !!this._ctx
      const ctx = isNewApi ? this._ctx : wx.createCanvasContext('dotCanvas', this)
      const w = this._canvasW
      const h = this._canvasH
      const cols = this._cols
      const rows = this._rows
      const r = this._r
      const gapX = this._gapX
      const gapY = this._gapY
      const bgColor = this.data.themeBg || '#0d0d0d'
      const isDark = bgColor === '#0d0d0d' || this._isDark(bgColor)
      const dotColor = isDark ? 'rgba(255, 68, 68, 0.12)' : 'rgba(0, 150, 180, 0.18)'
      const clockColor = isDark ? '#F9FAFB' : '#111827'
      const clockMeta = isDark ? 'rgba(249,250,251,0.5)' : 'rgba(17,24,39,0.5)'
      const clockGlow = isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.15)'
      this.setData({ timeColor: clockColor, timeColorMeta: clockMeta, timeGlowColor: clockGlow })

      ctx.clearRect(0, 0, w, h)
      if (isNewApi) {
        ctx.fillStyle = bgColor
      } else {
        ctx.setFillStyle(bgColor)
      }
      ctx.fillRect(0, 0, w, h)

      for (let row = 0; row < rows; row++) {
        for (let col = 0; col < cols; col++) {
          const x = col * (r * 2 + gapX) + r + gapX / 2
          const y = row * (r * 2 + gapY) + r + gapY / 2
          ctx.beginPath()
          ctx.arc(x, y, r, 0, Math.PI * 2)
          if (isNewApi) {
            ctx.fillStyle = dotColor
          } else {
            ctx.setFillStyle(dotColor)
          }
          ctx.fill()
        }
      }

      if (!isNewApi) ctx.draw()
    },

    _isDark(hex) {
      if (!hex || !hex.startsWith('#')) return true
      const c = hex.replace('#', '')
      if (c.length < 6) return true
      const rv = parseInt(c.substr(0, 2), 16)
      const gv = parseInt(c.substr(2, 2), 16)
      const bv = parseInt(c.substr(4, 2), 16)
      return (rv * 299 + gv * 587 + bv * 114) / 1000 < 128
    },

    // ==================== 动画启停 ====================

    _startAnim() {
      if (!this._ready) return
      if (this._animTimer) return
      if (!this._canvasNode) {
        // 等 canvas node 就绪后再开始动画
        const tryStart = () => {
          if (this._canvasNode) {
            this._animLoop()
          }
        }
        wx.nextTick(tryStart)
        return
      }
      this._animLoop()
    },

    _stopAnim() {
      if (this._animTimer) {
        if (this._canvasNode) this._canvasNode.cancelAnimationFrame(this._animTimer)
        this._animTimer = null
      }
    },

    _cleanup() {
      this._stopAnim()
      if (this._clockTimer) clearInterval(this._clockTimer)
      this._ready = false
    },

    // ==================== 动画主循环（requestAnimationFrame）====================

    _animLoop() {
      const loop = () => {
        if (!this._ready || !this._ctx) return
        const ctx = this._ctx
        const w = this._canvasW
        const h = this._canvasH
        const cols = this._cols
        const rows = this._rows
        const r = this._r
        const gapX = this._gapX
        const gapY = this._gapY
        const bgColor = this.data.themeBg || '#0d0d0d'
        const freq = this._freq || []
        const freqLen = freq.length
        const isDark = bgColor === '#0d0d0d' || this._isDark(bgColor)
        const baseR = isDark ? 255 : 0
        const baseG = isDark ? 68 : 160
        const baseB = isDark ? 68 : 200

        ctx.clearRect(0, 0, w, h)
        ctx.fillStyle = bgColor
        ctx.fillRect(0, 0, w, h)

        for (let row = 0; row < rows; row++) {
          for (let col = 0; col < cols; col++) {
            const x = col * (r * 2 + gapX) + r + gapX / 2
            const y = row * (r * 2 + gapY) + r + gapY / 2

            const freqIdx = freqLen > 0 ? Math.floor((col / cols) * freqLen) : 0
            const amp = freq[freqIdx] || 0
            const heightRatio = 1 - row / Math.max(rows - 1, 1)
            const targetR = r + amp * r * 2.2 * heightRatio

            this._dotSizes[row][col] = this._dotSizes[row][col] * 0.72 + targetR * 0.28
            const curR = this._dotSizes[row][col]
            const intensity = Math.min(1, curR / (r * 3.2))

            ctx.beginPath()
            ctx.arc(x, y, Math.max(0.5, curR), 0, Math.PI * 2)

            if (intensity > 0.65) {
              const gBright = Math.floor(baseG + intensity * 80)
              ctx.fillStyle = `rgba(${baseR},${gBright},${baseB},${(0.4 + intensity * 0.6).toFixed(2)})`
            } else {
              const gDark = Math.floor(baseG * 0.3 + intensity * baseG)
              ctx.fillStyle = `rgba(${baseR},${gDark},${baseB},${(0.15 + intensity * 0.5).toFixed(2)})`
            }
            ctx.fill()
          }
        }

        this._animTimer = this._canvasNode.requestAnimationFrame(loop)
      }
      this._animTimer = this._canvasNode.requestAnimationFrame(loop)
    },

    // ==================== 父组件调用：更新频率数据 ====================

    updateFrequencyData(freqArray) {
      this._freq = Array.isArray(freqArray) ? freqArray : []
    },
  },
})
