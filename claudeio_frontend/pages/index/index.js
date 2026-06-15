Page({
  data: {
    timeDigits: ['0', '0', '0', '0'],
    dayOfWeek: '',
    fullDate: '',
    timer: null,
    isPlaying: false,
    isLoggedIn: false,
    theme: 'dark',
    showPlayerView: false,
    waveformBars: [20, 30, 45, 60, 80, 50, 40, 65, 90, 75, 45, 30, 20, 50, 70, 85, 60, 40, 25, 45, 75, 95, 80, 55, 35, 60, 85, 70, 40, 25, 50, 75, 90, 65, 45, 30, 20, 40, 60, 45],
    currentTrackIndex: 0,
    showQueue: false,
    queueHeight: 0,
    currentTrack: '',
    queue: [],
    lyricLines: [],
    currentLyricIndex: -1,
    currentTime: 0,
    duration: 0,
    currentTimeStr: '0:00',
    durationStr: '0:00',
    seeking: false,
    volume: 80,
    chatInput: '',
    messages: [],
    isAiThinking: false, // 控制等待气泡显示的标志位
    playStartTime: null,
    currentSong: null,
    currentGenre: 'default',  // 当前风格
    genreCache: {},           // 本地缓存 {songId: genre}
    genreTransitioning: false ,// 切换动画状态
    containerClass: 'container genre-default' ,
    isTyping: false,      // 控制专注模式的开关
    keyboardHeight: 0,    // 实时记录键盘高度
    playerTab: 0, // 0: 歌词, 1: 历史记录
    playHistory: [], // 历史记录数据
    isHistoryLoaded: false, // 避免每次滑动都重复请求
    swiperDuration: 300, // 动态控制 swiper 的左右切换动画时间
  },

  canvas: null,
  ctx: null,
  systemInfo: null,
  pixelSize: 4,
  gap: 2,
  digitWidth: 5,
  digitHeight: 7,
  animationId: null,

  onLoad: function () {
    this.systemInfo = wx.getSystemInfoSync();
    this.updateTime();
    const timer = setInterval(() => { this.updateTime(); }, 1000);
    this.setData({ timer });

    const userId = wx.getStorageSync('music_userId');
    if (userId) {
      this.setData({ isLoggedIn: true });
      // 加载 AI 问候
      this.loadGreeting(userId);
    }

    // 从 localStorage 加载风格缓存
    const cachedGenres = wx.getStorageSync('genre_cache') || {};
    this.setData({ genreCache: cachedGenres });

    this.audio = wx.createInnerAudioContext();
    this.audio.volume = (this.data.volume || 80) / 100;

    // 设置音频加载超时（对大文件很重要）
    this.audio.obeyMuteSwitch = false; // 不遵循静音开关

    this.audio.onPlay(() => {
      this.setData({ isPlaying: true });
      this.recordPlayStart();
      console.log('音频开始播放');
    });
    this.audio.onPause(() => {
      this.setData({ isPlaying: false });
      this.recordPlayCompletion(false);
    });
    this.audio.onStop(() => {
      this.setData({ isPlaying: false });
      this.recordPlayCompletion(false);
    });
    this.audio.onEnded(() => {
      this.recordPlayCompletion(true);
      this.playNext();
    });
    this.audio.onError((err) => {
      console.error('audio error', err);

      // 根据不同错误类型提供更详细的提示
      let errorMessage = '播放失败';

      if (err && err.errMsg) {
        if (err.errMsg.includes('timeout')) {
          errorMessage = '加载超时，请检查网络或稍后重试';
        } else if (err.errMsg.includes('network')) {
          errorMessage = '网络错误，请检查网络连接';
        } else if (err.errMsg.includes('format')) {
          errorMessage = '音频格式不支持';
        } else if (err.errMsg.includes('decode')) {
          errorMessage = '音频解码失败';
        }
      }

      wx.showToast({
        title: errorMessage,
        icon: 'none',
        duration: 3000
      });

      this.setData({ isPlaying: false });
      this.recordPlayCompletion(false);
    });
    this.audio.onTimeUpdate(() => { this.syncLyric(); if (this.data.seeking) return; const ct = this.audio.currentTime || 0; const du = this.audio.duration || 0; this.setData({ currentTime: ct, duration: du, currentTimeStr: this.formatTime(ct), durationStr: this.formatTime(du) }); });
    this.audio.onCanplay(() => {
      const du = this.audio.duration || 0;
      if (du) {
        this.setData({ duration: du, durationStr: this.formatTime(du) });
        console.log('音频可以播放，时长:', this.formatTime(du));
      }
    });

    // 添加等待播放事件（音频正在缓冲）
    this.audio.onWaiting(() => {
      console.log('音频缓冲中...');
    });

    // 添加可以播放事件（缓冲完成）
    this.audio.onSeeking(() => {
      console.log('音频跳转中...');
    });

    this.audio.onSeeked(() => {
      console.log('音频跳转完成');
    });
  },

// 1. 新增 onShow 生命周期
onShow: function () {
  const userId = wx.getStorageSync('music_userId');
  // 若缓存有 userId 且当前未登录，说明刚从登录页返回
  if (userId && !this.data.isLoggedIn) {
    this.setData({ isLoggedIn: true });
    this.loadGreeting(userId); // 重新加载队列和问候
  } 
  // 若缓存已被清空（如已退出登录），重置状态
  else if (!userId && this.data.isLoggedIn) {
    this.setData({ isLoggedIn: false });
  }
},

  loadGreeting: function(userId) {
    wx.request({
      url: `${getApp().globalData.apiBase}/greeting`,
      data: { userId: Number(userId) },
      success: (res) => {
        const reply = res.data?.reply || '';
        const songs = res.data?.songs || [];

        // 添加 AI 问候消息
        const greetingMsg = {
          id: Date.now(),
          role: 'assistant',
          author: 'CLAUDIO',
          content: reply
        };

        const messages = [greetingMsg];

        // 不再把歌曲作为单独的消息添加，只设置播放队列
        if (songs.length) {
          // 设置播放队列
          const queue = songs.map(s => ({
            id: s.id,
            title: s.name,
            artist: s.artist || 'Unknown'
          }));

          // 批量预加载风格
          this.batchLoadGenres(queue);

          this.setData({
            messages,
            queue,
            currentTrackIndex: 0,
            currentTrack: queue[0].title + ' - ' + queue[0].artist,
            showPlayerView: false
          });
        } else {
          this.setData({ messages });
        }

        this.scrollChatToBottom();
      },
      fail: () => {
        console.log('加载问候失败');
      }
    });
  },

  onChatInput: function (e) {
    this.setData({ chatInput: e.detail.value });
  },

  sendChat: function () {
    const message = (this.data.chatInput || '').trim();
    if (!message) return;
    const userId = wx.getStorageSync('music_userId');
    if (!userId) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    const userMsg = { id: Date.now(), role: 'user', author: 'YOU', content: message };
    // 1. 发送前：追加用户消息，开启 AI 思考状态并滚动到底部
    this.setData({ 
      messages: [...this.data.messages, userMsg], 
      chatInput: '',
      isAiThinking: true 
    }, () => {
      this.scrollChatToBottom();
    });

    wx.request({
      url: `${getApp().globalData.apiBase}/chat`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { message, userId: Number(userId) },
      success: (res) => {
        // 2. 请求返回：首要任务是关闭 AI 思考状态
        this.setData({ isAiThinking: false });
        
        const reply = res.data?.reply || '';
        const songs = res.data?.songs || [];
        const songId = res.data?.songId;
        const action = res.data?.action;
        const newUserId = res.data?.newUserId;
        const newNickname = res.data?.newNickname;

        // ========== 播放控制指令处理 ==========

        // 暂停播放
        if (action === 'pause_music') {
          if (this.audio) {
            this.audio.pause();
          }
          const assistantMsg = {
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content: reply
          };
          this.setData({
            messages: [...this.data.messages, assistantMsg]
          }, () => {
            this.scrollChatToBottom();
          });
          return;
        }

        // 继续播放
        if (action === 'resume_music') {
          if (this.audio && this.audio.src) {
            this.audio.play();
          } else {
            const assistantMsg = {
              id: Date.now() + 1,
              role: 'assistant',
              author: 'CLAUDIO',
              content: '当前没有正在播放的歌曲，请先搜索或选择歌曲。'
            };
            this.setData({
              messages: [...this.data.messages, assistantMsg]
            }, () => {
              this.scrollChatToBottom();
            });
            return;
          }
          const assistantMsg = {
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content: reply
          };
          this.setData({
            messages: [...this.data.messages, assistantMsg]
          }, () => {
            this.scrollChatToBottom();
          });
          return;
        }

        // 下一首
        if (action === 'play_next') {
          this.playNext();
          const assistantMsg = {
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content: reply
          };
          this.setData({
            messages: [...this.data.messages, assistantMsg]
          }, () => {
            this.scrollChatToBottom();
          });
          return;
        }

        // 上一首
        if (action === 'play_previous') {
          this.playPrev();
          const assistantMsg = {
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content: reply
          };
          this.setData({
            messages: [...this.data.messages, assistantMsg]
          }, () => {
            this.scrollChatToBottom();
          });
          return;
        }

        // 调整音量
        if (action && action.startsWith('set_volume:')) {
          const volume = parseInt(action.substring(11));
          if (!isNaN(volume) && volume >= 0 && volume <= 100) {
            if (this.audio) {
              this.audio.volume = volume / 100;
            }
            this.setData({ volume });
          }
          const assistantMsg = {
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content: reply
          };
          this.setData({
            messages: [...this.data.messages, assistantMsg]
          }, () => {
            this.scrollChatToBottom();
          });
          return;
        }

        // 处理退出登录
        if (action === 'logout') {
          const assistantMsg = { id: Date.now() + 1, role: 'assistant', author: 'CLAUDIO', content: reply };
          this.setData({ messages: [...this.data.messages, assistantMsg] }, () => {
            this.scrollChatToBottom();
          });

          // 清理播放器状态
          if (this.audio) {
            this.audio.stop();
            this.audio.src = '';
            this.audio.destroy();
            this.audio = null;
          }

          // 重置所有播放相关状态
          this.setData({
            queue: [],
            currentTrack: '',
            currentTrackIndex: 0,
            currentSong: null,
            isPlaying: false,
            lyricLines: [],
            currentLyricIndex: -1,
            currentTime: 0,
            duration: 0,
            currentTimeStr: '0:00',
            durationStr: '0:00',
            showPlayerView: false,
            showQueue: false
          });

          wx.removeStorageSync('music_userId');
          wx.showToast({ title: '已退出登录', icon: 'success' });
          setTimeout(() => {
            wx.redirectTo({ url: '/pages/login/login' });
          }, 1500);
          return;
        }

        // 处理切换账号
        if (action === 'switch_account' && newUserId) {
          const assistantMsg = { id: Date.now() + 1, role: 'assistant', author: 'CLAUDIO', content: reply };
          this.setData({ messages: [...this.data.messages, assistantMsg] }, () => {
            this.scrollChatToBottom();
          });
          wx.setStorageSync('music_userId', newUserId);
          wx.showToast({ title: '切换成功', icon: 'success' });
          setTimeout(() => {
            // 清空对话和播放队列
            this.setData({
              messages: [],
              queue: [],
              currentTrack: '',
              showPlayerView: false
            });
            // 重新加载欢迎消息
            this.loadGreeting(newUserId);
          }, 1500);
          return;
        }

        const assistantMsg = { id: Date.now() + 1, role: 'assistant', author: 'CLAUDIO', content: reply };
        const nextMessages = [...this.data.messages, assistantMsg];

        // 不再把歌曲作为单独的消息添加，只设置播放队列
        if (songs.length) {
          const queue = songs.map(s => ({ id: s.id, title: s.name, artist: s.artist || 'Unknown' }));

          // 批量预加载风格
          this.batchLoadGenres(queue);

          this.setData({
            queue,
            currentTrackIndex: 0,
            currentTrack: queue[0].title + ' - ' + queue[0].artist,
            showPlayerView: true,
            swiperDuration: 0, // 【新增】：自动唤醒时动画时长设为 0
            playerTab: 0,       // 【新增】：自动唤醒时强制回归歌词页
            messages: nextMessages
          }, () => {
            this.scrollChatToBottom();
            this.loadAndPlayTrack(queue[0], 0);
          });
        } else {
          this.setData({ messages: nextMessages }, () => {
            this.scrollChatToBottom();
          });
          if (songId) {
            this.loadAndPlayById(songId);
          }
        }
      },
      fail: () => {
        // 3. 请求失败：兜底关闭状态
        this.setData({ isAiThinking: false });
        wx.showToast({ title: '对话失败', icon: 'none' });
      }
    });


  },

// 控制聊天视图滚动逻辑
  scrollChatToBottom: function () {
    // 如果处于等待状态，优先滚动到等待气泡
    if (this.data.isAiThinking) {
      setTimeout(() => {
        this.setData({ chatScrollIntoView: 'msg-thinking' });
      }, 50);
      return;
    }
    
    const messages = this.data.messages;
    if (messages.length === 0) return;
    const lastMsg = messages[messages.length - 1];
    setTimeout(() => {
      this.setData({ chatScrollIntoView: 'msg-' + lastMsg.id });
    }, 50);
  },

  beat: 0,
  lastLyricIdx: -1,

  syncLyric: function () {
    if (!this.audio) return;
    const t = this.audio.currentTime * 1000;
    const lines = this.data.lyricLines;
    if (!lines || !lines.length) return;
    let idx = -1;
    for (let i = 0; i < lines.length; i++) { if (lines[i].time <= t) idx = i; else break; }
    if (idx !== this.data.currentLyricIndex) { this.setData({ currentLyricIndex: idx }); this.beat = 1; }
  },

  loadLyric: function (songId) {
    const apiBase = getApp().globalData.apiBase;
    wx.request({ url: `${apiBase}/lyric/new`, data: { id: songId }, success: (res) => {
      const yrc = res.data && res.data.yrc && res.data.yrc.lyric;
      const lrc = res.data && res.data.lrc && res.data.lrc.lyric;
      const lines = this.parseLrc(lrc) || this.parseLyric(yrc) || [];
      this.setData({ lyricLines: lines, currentLyricIndex: -1 });
    }, fail: () => this.setData({ lyricLines: [], currentLyricIndex: -1 }) });
  },

  parseLyric: function (yrc) {
    if (!yrc) return null;
    const lines = [];
    const rawLines = yrc.split('\n');
    const headRe = /^\[(\d+),(\d+)\](.*)$/;
    const wordRe = /\((\d+),(\d+),\d+\)([^\(]*)/g;
    rawLines.forEach(raw => {
      if (raw.startsWith('{')) return;
      const m = raw.match(headRe);
      if (!m) return;
      const time = parseInt(m[1], 10);
      const rest = m[3];
      let text = '';
      let wm;
      while ((wm = wordRe.exec(rest)) !== null) text += wm[3];
      if (!text) text = rest;
      if (text.trim()) lines.push({ time, text });
    });
    return lines.length ? lines : null;
  },

  parseLrc: function (lrc) {
    if (!lrc) return null;
    const lines = [];
    const re = /\[(\d+):(\d+)(?:\.(\d+))?\]/g;
    lrc.split('\n').forEach(line => {
      let m;
      const matches = [];
      while ((m = re.exec(line)) !== null) matches.push(m);
      if (!matches.length) return;
      const text = line.replace(/\[[^\]]+\]/g, '').trim();
      if (!text) return;
      matches.forEach(mm => {
        const min = parseInt(mm[1], 10);
        const sec = parseInt(mm[2], 10);
        const ms = mm[3] ? parseInt((mm[3] + '00').slice(0, 3), 10) : 0;
        lines.push({ time: (min * 60 + sec) * 1000 + ms, text });
      });
    });
    lines.sort((a, b) => a.time - b.time);
    return lines.length ? lines : null;
  },

  loadAndPlayTrack: function (track, index) {
    if (!track) return;
    const apiBase = getApp().globalData.apiBase;
    const userId = wx.getStorageSync('music_userId');
    console.log('准备播放队列歌曲', track);

    // 检查本地缓存的风格
    const cachedGenre = this.data.genreCache[track.id];
    if (cachedGenre) {
      console.log('从缓存获取风格:', cachedGenre);
      this.switchGenre(cachedGenre);
    } else {
      // 先切换到默认风格
      this.switchGenre('default');
      // 异步获取风格
      this.fetchGenreAsync(track.id, track.title, track.artist);
    }

    wx.request({
      url: `${apiBase}/play-url`,
      data: { songId: track.id, userId },
      timeout: 30000, // 设置 30 秒超时（加载大文件需要更长时间）
      success: (res) => {
        const url = res.data && res.data.url;
        if (!url) {
          wx.showToast({ title: '无版权或无法播放', icon: 'none' });
          return;
        }

        console.log('播放 URL 获取成功:', url.substring(0, 80) + '...');

        this.setData({
          currentTrackIndex: index,
          currentTrack: track.title + ' - ' + track.artist,
          currentSong: {
            id: track.id,
            name: track.title,
            artist: track.artist
          }
        });

        // 设置音频源
        this.audio.src = url;

        // 添加播放前的日志
        console.log('开始播放音乐，文件大小预计:', res.data.size || '未知');

        this.audio.play();
        this.loadLyric(track.id);
      },
      fail: (err) => {
        console.error('获取播放 URL 失败:', err);
        wx.showToast({
          title: '获取播放链接失败，请重试',
          icon: 'none',
          duration: 2000
        });
      }
    });
  },

  loadAndPlayById: function (songId) {
    const track = this.data.queue.find(item => item.id === songId) || { id: songId, title: '未知歌曲', artist: '未知艺术家' };
    const index = this.data.queue.findIndex(item => item.id === songId);
    this.loadAndPlayTrack(track, index >= 0 ? index : this.data.currentTrackIndex);
  },

  recordPlayStart: function() {
    this.setData({
      playStartTime: Date.now()
    });
    console.log('播放开始记录', this.data.currentSong);
  },

  recordPlayCompletion: function(completed) {
    if (!this.data.playStartTime || !this.data.currentSong) {
      return;
    }

    const duration = Math.floor((Date.now() - this.data.playStartTime) / 1000);
    const userId = wx.getStorageSync('music_userId');

    if (duration < 5) {
      console.log('播放时长不足5秒，不记录');
      this.setData({ playStartTime: null });
      return;
    }

    const apiBase = getApp().globalData.apiBase;
    wx.request({
      url: `${apiBase}/play-history`,
      method: 'POST',
      data: {
        userId: Number(userId),
        songId: this.data.currentSong.id,
        songName: this.data.currentSong.name,
        artist: this.data.currentSong.artist,
        duration: duration,
        completed: completed
      },
      success: (res) => {
        console.log('播放记录已保存', res.data, this.data.currentSong);
        
        // 播放记录保存成功后，处理状态
        // 如果用户正好正开着抽屉并且停留在历史记录页，直接静默刷新列表
        if (this.data.showPlayerView && this.data.playerTab === 1) {
          this.fetchPlayHistory();
        } else {
          // 否则只需将标志位重置，等用户下次滑动过去时就会自动获取最新数据
          this.setData({ isHistoryLoaded: false });
        }
      },
      fail: (err) => {
        console.error('播放记录保存失败', err);
      }
    });

    this.setData({ playStartTime: null });
  },

  playNext: function () { const { queue, currentTrackIndex } = this.data; if (!queue.length) return; const next = (currentTrackIndex + 1) % queue.length; const track = queue[next]; this.audio.stop(); this.audio.src = ''; this.setData({ currentTrackIndex: next, currentTrack: track.title + ' - ' + track.artist, lyricLines: [], currentLyricIndex: -1, currentTime: 0, duration: 0, currentTimeStr: '0:00', durationStr: '0:00' }, () => { this.loadAndPlayTrack(track, next); }); },
  playPrev: function () { const { queue, currentTrackIndex } = this.data; if (!queue.length) return; const prev = (currentTrackIndex - 1 + queue.length) % queue.length; const track = queue[prev]; this.audio.stop(); this.audio.src = ''; this.setData({ currentTrackIndex: prev, currentTrack: track.title + ' - ' + track.artist, lyricLines: [], currentLyricIndex: -1, currentTime: 0, duration: 0, currentTimeStr: '0:00', durationStr: '0:00' }, () => { this.loadAndPlayTrack(track, prev); }); },
  formatTime: function (sec) { sec = Math.max(0, Math.floor(sec || 0)); const m = Math.floor(sec / 60); const s = sec % 60; return m + ':' + (s < 10 ? '0' + s : s); },
  onSeeking: function (e) { this.setData({ seeking: true, currentTime: e.detail.value, currentTimeStr: this.formatTime(e.detail.value) }); },
  onSeek: function (e) { const v = e.detail.value; if (this.audio && this.audio.src) this.audio.seek(v); this.setData({ seeking: false, currentTime: v, currentTimeStr: this.formatTime(v) }); },
  onVolumeChange: function (e) { const v = e.detail.value; if (this.audio) this.audio.volume = v / 100; this.setData({ volume: v }); },
  seekToLyric: function (e) { const timeMs = e.currentTarget.dataset.time; const idx = e.currentTarget.dataset.index; if (!this.audio || !this.audio.src) return; const sec = timeMs / 1000; this.audio.seek(sec); if (this.audio.paused) this.audio.play(); this.setData({ currentLyricIndex: idx, currentTime: sec, currentTimeStr: this.formatTime(sec) }); },
  onReady: function () { const query = wx.createSelectorQuery(); query.select('#clockCanvas').fields({ node: true, size: true }).exec((res) => { if (!res[0]) return; const canvas = res[0].node; const ctx = canvas.getContext('2d'); const dpr = this.systemInfo ? this.systemInfo.pixelRatio : 1; canvas.width = res[0].width * dpr; canvas.height = res[0].height * dpr; ctx.scale(dpr, dpr); this.canvas = canvas; this.ctx = ctx; this.startClockAnimation(); }); },
  onUnload: function () { if (this.data.timer) clearInterval(this.data.timer); if (this.animationId) this.canvas.cancelAnimationFrame(this.animationId); if (this.audio) this.audio.destroy(); },

  togglePlay: function () { const { queue, currentTrackIndex, isPlaying } = this.data; if (!queue.length) return; if (isPlaying) { this.audio.pause(); this.setData({ showPlayerView: false }); return; } if (!this.audio.src) { const track = queue[currentTrackIndex]; this.loadAndPlayTrack(track, currentTrackIndex); } else { this.audio.play(); } 
  this.setData({ 
    showPlayerView: true,
    swiperDuration: 0,  // 【新增】：进入时动画时长设为 0
    playerTab: 0        // 【新增】：强制回归歌词页
   }); },

   closePlayerView: function () { 
    this.setData({ showPlayerView: false }); 
    // 【修改】：延迟 410ms（略大于 WXSS 中定义的 0.4s 抽屉下滑过渡动画）
    // 当抽屉完全滑出屏幕视线后，在后台悄悄把状态拨回歌词页，消除任何穿帮可能
    setTimeout(() => {
      this.setData({
        swiperDuration: 0,
        playerTab: 0,
        isHistoryLoaded: false
      });
    }, 410);
  },

  // 打开歌词/播放器抽屉的方法
// 打开歌词/播放器抽屉的方法
openPlayerView: function () {
  // 判断当前有正在播放的歌曲，或播放队列里有歌，才允许打开抽屉
  if (this.data.currentTrack || (this.data.queue && this.data.queue.length > 0)) {
    this.setData({ 
      showPlayerView: true,
      swiperDuration: 0,      // 【修改】：进入时动画时长设为 0，瞬间切回
      playerTab: 0,           // 【修改】：强制回归歌词页
      isHistoryLoaded: false  // 每次打开抽屉重置数据加载状态，保证切过去时能拉取最新数据
    });
  } else {
    wx.showToast({
      title: '当前没有正在播放的歌曲',
      icon: 'none'
    });
  }
},

tapQueueItem: function (e) { 
  const index = e.currentTarget.dataset.index; 
  const { queue, currentTrackIndex } = this.data; 
  if (index === currentTrackIndex) return; 
  const above = queue.slice(0, index); 
  const clicked = queue[index]; 
  const below = queue.slice(index + 1); 
  const newQueue = [clicked, ...below, ...above]; 
  this.audio.stop(); 
  this.audio.src = ''; 
  this.setData({ 
    queue: newQueue, 
    currentTrackIndex: 0, 
    currentTrack: clicked.title + ' - ' + clicked.artist, 
    showPlayerView: true,
    swiperDuration: 0,  // 【新增】：进入时动画时长设为 0
    playerTab: 0        // 【新增】：强制回归歌词页
  }, () => { 
    this.loadAndPlayTrack(clicked, 0); 
  }); 
},


  deleteQueueItem: function (e) { const index = e.currentTarget.dataset.index; const { queue, currentTrackIndex } = this.data; const newQueue = queue.filter((_, i) => i !== index); const newIndex = index < currentTrackIndex ? currentTrackIndex - 1 : currentTrackIndex; this.setData({ queue: newQueue, currentTrackIndex: Math.min(newIndex, newQueue.length - 1) }); },
  toggleQueue: function () { const next = !this.data.showQueue; this.setData({ showQueue: next, queueHeight: next ? 120 : 0 }); },

  goToLogin: function () { 
    if (this.data.isLoggedIn) {
      wx.navigateTo({ url: '/pages/my/my' });
    } else {
      wx.navigateTo({ url: '/pages/login/login' }); 
    }
  },
  toggleTheme: function (e) { const targetTheme = e.currentTarget.dataset.theme; if (targetTheme && targetTheme !== this.data.theme) this.setData({ theme: targetTheme }); },
  updateTime: function () { const now = new Date(); const hours = now.getHours().toString().padStart(2, '0'); const minutes = now.getMinutes().toString().padStart(2, '0'); const timeString = hours + minutes; const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']; const dayOfWeek = days[now.getDay()]; const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']; const day = now.getDate().toString().padStart(2, '0'); const month = months[now.getMonth()]; const year = now.getFullYear(); const fullDate = `${day} ${month} ${year}`; if (this.data.timeDigits.join('') !== timeString || this.data.fullDate !== fullDate) this.setData({ timeDigits: timeString.split(''), dayOfWeek, fullDate }); },
  startClockAnimation: function () { const render = () => { this.drawPixelClock(); this.animationId = this.canvas.requestAnimationFrame(render); }; render(); },
  particles: [],
  maxParticles: 224,
  particlesInitialized: false,
  calculateTargets: function() {
    const { isPlaying, timeDigits } = this.data;
    const { pixelSize, gap, digitHeight } = this;
    const dpr = this.systemInfo ? this.systemInfo.pixelRatio : 1;
    const canvasWidth = this.canvas.width / dpr;
    const canvasHeight = this.canvas.height / dpr;
    this.particles.forEach(p => p.active = false);
    let pIndex = 0;
    if (!isPlaying) {
      const charPatterns = { '0': [0x3E, 0x41, 0x41, 0x41, 0x3E], '1': [0x00, 0x42, 0x7F, 0x40, 0x00], '2': [0x42, 0x61, 0x51, 0x49, 0x46], '3': [0x22, 0x41, 0x49, 0x49, 0x36], '4': [0x18, 0x14, 0x12, 0x7F, 0x10], '5': [0x27, 0x45, 0x45, 0x45, 0x39], '6': [0x3E, 0x49, 0x49, 0x49, 0x32], '7': [0x01, 0x01, 0x71, 0x09, 0x07], '8': [0x36, 0x49, 0x49, 0x49, 0x36], '9': [0x26, 0x49, 0x49, 0x49, 0x3E], ':': [0x00, 0x24, 0x00] };
      const chars = [timeDigits[0], timeDigits[1], ':', timeDigits[2], timeDigits[3]];
      let totalUnits = 0; chars.forEach((char, index) => { totalUnits += (char === ':' ? 3 : 5); if (index < chars.length - 1) totalUnits += 1; });
      const totalWidth = totalUnits * (pixelSize + gap); let startX = (canvasWidth - totalWidth) / 2; const startY = (canvasHeight - digitHeight * (pixelSize + gap)) / 2;
      chars.forEach((char) => { const pattern = charPatterns[char]; const w = char === ':' ? 3 : 5; for (let x = 0; x < w; x++) { for (let y = 0; y < 7; y++) { if ((pattern[x] >> y) & 1) { if (pIndex < this.maxParticles) { const p = this.particles[pIndex++]; p.targetX = startX + x * (pixelSize + gap); p.targetY = startY + y * (pixelSize + gap); p.active = true; } } } } startX += (w + 1) * (pixelSize + gap); });
    } else {
      const numBars = 32; const maxBarHeight = 7; const totalWidth = numBars * (pixelSize + gap) - gap; const startX = (canvasWidth - totalWidth) / 2; const startY = (canvasHeight - maxBarHeight * (pixelSize + gap)) / 2; const now = Date.now(); this.beat = (this.beat || 0) * 0.92; const beat = this.beat; for (let i = 0; i < numBars; i++) { const center = (numBars - 1) / 2; const distFromCenter = Math.abs(i - center) / center; const envelope = 1 - distFromCenter * 0.3; const noise = Math.sin(now * 0.008 + i * 0.5) * 0.5 + 0.5; const noise2 = Math.cos(now * 0.004 + i * 1.2) * 0.5 + 0.5; const base = (noise * 0.6 + noise2 * 0.4) * envelope; const beatBoost = beat * (0.6 + Math.sin(i * 1.7 + now * 0.02) * 0.4); let barHeight = Math.floor((base + beatBoost) * maxBarHeight) + 1; if (barHeight > maxBarHeight) barHeight = maxBarHeight; if (barHeight < 1) barHeight = 1; for (let y = 0; y < maxBarHeight; y++) { if (pIndex < this.maxParticles) { const p = this.particles[pIndex++]; p.targetX = startX + i * (pixelSize + gap); if (y < barHeight) { p.targetY = startY + (maxBarHeight - 1 - y) * (pixelSize + gap); p.active = true; } else { p.targetY = startY + (maxBarHeight - barHeight) * (pixelSize + gap); p.active = false; } } } }
    }
  },
  drawPixelClock: function () { if (!this.ctx) return; if (this.particles.length === 0) this.particles = Array.from({ length: this.maxParticles }).map(() => ({ x: 0, y: 0, targetX: 0, targetY: 0, active: false, alpha: 0 })); this.calculateTargets(); const { ctx, pixelSize } = this; const { theme } = this.data; const dpr = this.systemInfo ? this.systemInfo.pixelRatio : 1; const canvasWidth = this.canvas.width / dpr; const canvasHeight = this.canvas.height / dpr; ctx.clearRect(0, 0, canvasWidth, canvasHeight); ctx.fillStyle = theme === 'dark' ? '#FFFFFF' : '#000000'; this.particles.forEach(p => { if (!this.particlesInitialized) { p.x = p.targetX; p.y = p.targetY; p.alpha = p.active ? 1 : 0; } else { p.x += (p.targetX - p.x) * 0.15; p.y += (p.targetY - p.y) * 0.15; p.alpha += ((p.active ? 1 : 0) - p.alpha) * 0.15; } if (p.alpha > 0.01) { ctx.globalAlpha = p.alpha; ctx.fillRect(p.x, p.y, pixelSize, pixelSize); } }); ctx.globalAlpha = 1.0; this.particlesInitialized = true; },

  /**
   * 切换音乐风格主题
   */
  switchGenre: function(genre) {
    if (!genre || genre === this.data.currentGenre) {
      return; 
    }

    console.log('切换风格:', this.data.currentGenre, '->', genre);

    const query = wx.createSelectorQuery();
    query.select('.container').boundingClientRect();
    query.exec((res) => {
      if (!res || !res[0]) return;

      // 更新状态时，同步更新 containerClass 字符串
      this.setData({
        currentGenre: genre,
        genreTransitioning: true,
        containerClass: `container genre-${genre} genre-transitioning` // 新增
      });

      setTimeout(() => {
        this.setData({ 
          genreTransitioning: false,
          containerClass: `container genre-${genre}` // 新增
        });
      }, 1500);
    });
  },

  /**
   * 异步获取歌曲风格
   */
  fetchGenreAsync: function(songId, songName, artist) {
    const apiBase = getApp().globalData.apiBase;

    wx.request({
      url: `${apiBase}/genre`,
      data: {
        songId: songId,
        songName: songName,
        artist: artist
      },
      success: (res) => {
        if (res.data && res.data.genre) {
          const genre = res.data.genre;
          console.log('获取到风格:', genre, '来源:', res.data.source);

          // 更新缓存
          const genreCache = this.data.genreCache;
          genreCache[songId] = genre;
          this.setData({ genreCache });

          // 保存到本地存储
          wx.setStorageSync('genre_cache', genreCache);

          // 如果当前正在播放这首歌，切换风格
          if (this.data.currentSong && this.data.currentSong.id === songId) {
            this.switchGenre(genre);
          }
        }
      },
      fail: (err) => {
        console.error('获取风格失败:', err);
      }
    });
  },

  /**
   * 批量预加载播放队列风格
   */
  batchLoadGenres: function(songs) {
    if (!songs || songs.length === 0) return;

    const apiBase = getApp().globalData.apiBase;
    const songsData = songs.map(s => ({
      id: s.id,
      name: s.title || s.name,
      artist: s.artist
    }));

    wx.request({
      url: `${apiBase}/genres/batch`,
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: songsData,
      success: (res) => {
        if (res.data) {
          console.log('批量加载风格成功:', res.data);

          // 更新缓存
          const genreCache = this.data.genreCache;
          Object.assign(genreCache, res.data);
          this.setData({ genreCache });

          // 保存到本地存储
          wx.setStorageSync('genre_cache', genreCache);
        }
      },
      fail: (err) => {
        console.error('批量加载风格失败:', err);
      }
    });
  },

  // 输入框获取焦点（键盘弹起）
  onInputFocus: function (e) {
    const kbHeight = e.detail.height || 0;
    this.setData({
      isTyping: true,
      keyboardHeight: kbHeight
    }, () => {
      // 延迟一下，等待页面动画展开后再滚动到底部，保证最新消息可见
      setTimeout(() => {
        this.scrollChatToBottom();
      }, 300);
    });
  },

  // 输入框失去焦点（键盘收起）
  onInputBlur: function () {
    this.setData({
      isTyping: false,
      keyboardHeight: 0
    });
  },

  // 键盘高度实时变化（处理用户切换中英文/表情面板导致高度变化的情况）
  onKeyboardHeightChange: function (e) {
    if (this.data.isTyping) {
      this.setData({ keyboardHeight: e.detail.height || 0 });
      this.scrollChatToBottom();
    }
  },

  // --- 新增：播放器抽屉 Tab 切换及滑动逻辑 ---
  
// 点击 Tab 切换
switchPlayerTab: function(e) {
  const tab = parseInt(e.currentTarget.dataset.tab);
  this.setData({ 
    swiperDuration: 300, // 点击标签切换时，恢复 300ms 平滑动画
    playerTab: tab 
  });
},

// Swiper 左右滑动触发
onPlayerSwiperChange: function(e) {
  const tab = e.detail.current;
  this.setData({ 
    playerTab: tab,
    swiperDuration: 300 // 【新增】：用户手动滑动时，确保后续有完整的 300ms 滑动体验
  });
  
  // 首次滑动到历史记录时，加载数据
  if (tab === 1 && !this.data.isHistoryLoaded) {
    this.fetchPlayHistory();
  }
},

  // 获取历史记录 API 请求
  fetchPlayHistory: function() {
    const userId = wx.getStorageSync('music_userId');
    if (!userId) return;

    const apiBase = getApp().globalData.apiBase;
    wx.request({
      url: `${apiBase}/play-history?userId=${userId}`,
      method: 'GET',
      success: (res) => {
        if (res.data) {
          this.setData({ 
            playHistory: res.data,
            isHistoryLoaded: true 
          });
        }
      },
      fail: (err) => {
        console.error('获取历史记录失败', err);
      }
    });
  },

  // 点击历史记录播放
  playHistoryItem: function(e) {
    const item = e.currentTarget.dataset.item;
    const track = {
      id: item.songId,
      title: item.songName,
      artist: item.artist
    };
    
    // 加入队列并播放 (这里复用现有的 loadAndPlayTrack 逻辑)
    const { queue } = this.data;
    const existingIndex = queue.findIndex(q => q.id === track.id);
    
    if (existingIndex >= 0) {
      // 如果队列里已经有这首歌，直接切过去
      this.audio.stop();
      this.loadAndPlayTrack(queue[existingIndex], existingIndex);
    } else {
      // 将歌曲插入队列第一位并播放
      const newQueue = [track, ...queue];
      this.audio.stop();
      this.setData({ 
        queue: newQueue,
        playerTab: 0 // 切回歌词页
      }, () => {
        this.loadAndPlayTrack(track, 0);
      });
    }
  },

})



