// components/command-input/command-input.js

Component({
  properties: {},

  data: {
    inputValue: '',
    showCommandPanel: false,
    commandList: [
      { cmd: '/play [歌曲名]', desc: '播放指定歌曲' },
      { cmd: '/pause', desc: '暂停播放' },
      { cmd: '/next', desc: '下一首' },
      { cmd: '/prev', desc: '上一首' },
      { cmd: '/random', desc: '随机播放' },
      { cmd: '/loop', desc: '单曲循环' },
      { cmd: '/shuffle', desc: '随机播放' },
      { cmd: '/help', desc: '查看帮助' },
    ],
  },

  methods: {
    onInput(e) {
      const value = e.detail.value
      this.setData({ inputValue: value })
      // 检测 / 呼出命令面板
      if (value === '/') {
        this.setData({ showCommandPanel: true })
      } else {
        this.setData({ showCommandPanel: false })
      }
    },

    onConfirm(e) {
      const value = (e.detail.value || this.data.inputValue).trim()
      if (!value) return
      this.setData({ inputValue: '', showCommandPanel: false })
      this.triggerEvent('send', { value })
    },

    onSendTap() {
      const value = this.data.inputValue.trim()
      if (!value) return
      this.setData({ inputValue: '', showCommandPanel: false })
      this.triggerEvent('send', { value })
    },

    onCommandSelect(e) {
      const { value } = e.currentTarget.dataset
      this.setData({ inputValue: value, showCommandPanel: false })
      // 触发发送
      setTimeout(() => {
        this.triggerEvent('send', { value })
      }, 50)
    },

    onHidePanel() {
      this.setData({ showCommandPanel: false })
    },
  },
})
