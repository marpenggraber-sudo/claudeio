/**
 * utils/format.js
 * 时间与时长格式化工具
 */

/**
 * 格式化日期时间
 * @param {Date} date
 * @returns {string} e.g. "2026/04/27 16:30:00"
 */
function formatTime(date) {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()
  return `${[year, month, day].map(n => n.toString().padStart(2, '0')).join('/')} ${[hour, minute, second].map(n => n.toString().padStart(2, '0')).join(':')}`
}

/**
 * 格式化时长（秒 → mm:ss）
 * @param {number} seconds
 * @returns {string} e.g. "04:29"
 */
function formatDuration(seconds) {
  if (!seconds || isNaN(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

/**
 * 生成唯一 ID
 * @returns {string}
 */
function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 5)
}

// ============================================================
// 复古时钟格式化
// ============================================================

// 周几箭头样式映射（周一~周日）
const DAY_ARROWS = ['Mon▲', 'Tue▲', 'Wed▲', 'Thu▲', 'Fri▲', 'Sat▲', 'Sun▲']

/**
 * 格式化：获取当前日期显示（YYYY/MM/DD）
 * @returns {string} e.g. "2026/04/27"
 */
function getDisplayDate() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}/${m}/${d}`
}

/**
 * 格式化：获取当前时间（HH:MM:SS）
 * @returns {string} e.g. "14:30:05"
 */
function getDisplayTime() {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  return `${h}:${min}:${s}`
}

/**
 * 格式化：获取周几（箭头样式）
 * @returns {string} e.g. "Mon▲"
 */
function getDisplayDay() {
  const now = new Date()
  const dayIndex = now.getDay() === 0 ? 6 : now.getDay() - 1 // 周一=0, 周日=6
  return DAY_ARROWS[dayIndex]
}

module.exports = {
  formatTime,
  formatDuration,
  generateId,
  getDisplayDate,
  getDisplayTime,
  getDisplayDay,
}
