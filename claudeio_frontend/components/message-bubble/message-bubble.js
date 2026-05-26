// components/message-bubble/message-bubble.js
const { formatTime } = require('../../utils/format.js')

Component({
  properties: {
    message: {
      type: Object,
      value: {
        id: '',
        type: 'ai',   // 'user' | 'ai' | 'result' | 'system'
        content: '',
        song: null,
        timestamp: 0,
      },
    },
  },

  data: {
    formattedTime: '',
    isUser: false,
    isAI: false,
    isResult: false,
    isSystem: false,
  },

  observers: {
    message: function (msg) {
      if (!msg) return
      const t = msg.timestamp ? new Date(msg.timestamp) : new Date()
      this.setData({
        formattedTime: formatTime(t),
        isUser: msg.type === 'user',
        isAI: msg.type === 'ai',
        isResult: msg.type === 'result',
        isSystem: msg.type === 'system',
      })
    },
  },

  methods: {
    onPlaySong(e) {
      const { song } = e.currentTarget.dataset
      this.triggerEvent('playsong', { song })
    },
  },
})
