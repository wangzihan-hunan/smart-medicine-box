/**
 * 浏览器告警工具类
 *
 * 功能：
 * 1. 桌面通知 (Web Notifications API)：在系统桌面弹出通知，即使浏览器后台也能显示
 * 2. 声音提醒 (Web Audio API)：播放提示音，吸引用户注意
 *
 * 依赖：
 * - 浏览器需支持 Notification API
 * - 用户需授权通知权限
 */

/**
 * 通知权限状态枚举
 */
export const PermissionStatus = {
  GRANTED: 'granted',
  DENIED: 'denied',
  DEFAULT: 'default',
  UNSUPPORTED: 'unsupported'
}

/**
 * 检查当前浏览器是否支持通知功能
 * @returns {boolean}
 */
export function isNotificationSupported() {
  return typeof Notification !== 'undefined'
}

/**
 * 获取当前通知权限状态
 * @returns {string} PermissionStatus 枚举值
 */
export function getNotificationPermission() {
  if (!isNotificationSupported()) {
    return PermissionStatus.UNSUPPORTED
  }
  return Notification.permission
}

/**
 * 请求通知权限
 * @returns {Promise<string>} 权限状态
 */
export async function requestNotificationPermission() {
  if (!isNotificationSupported()) {
    console.warn('[BrowserAlert] 当前浏览器不支持桌面通知')
    return PermissionStatus.UNSUPPORTED
  }

  // 如果已授权或拒绝，直接返回
  if (Notification.permission !== PermissionStatus.DEFAULT) {
    return Notification.permission
  }

  try {
    const permission = await Notification.requestPermission()
    console.log(`[BrowserAlert] 通知权限: ${permission}`)
    return permission
  } catch (error) {
    console.error('[BrowserAlert] 请求通知权限失败:', error)
    return Notification.permission
  }
}

/**
 * 显示桌面通知
 * @param {Object} options - 通知选项
 * @param {string} options.title - 通知标题
 * @param {string} options.body - 通知内容
 * @param {string} [options.icon] - 通知图标 URL
 * @param {string} [options.tag] - 通知标签（相同 tag 会覆盖旧通知）
 * @param {boolean} [options.requireInteraction=false] - 是否需要用户手动关闭
 * @param {Function} [options.onClick] - 点击通知时的回调
 * @param {Function} [options.onClose] - 关闭通知时的回调
 * @returns {Notification|null} 通知对象或 null（不支持/无权限时）
 */
export function showNotification(options) {
  if (!isNotificationSupported()) {
    console.warn('[BrowserAlert] 不支持桌面通知')
    return null
  }

  if (Notification.permission !== PermissionStatus.GRANTED) {
    console.warn('[BrowserAlert] 未授权桌面通知')
    return null
  }

  try {
    const notification = new Notification(options.title || '通知', {
      body: options.body || '',
      icon: options.icon || '/favicon.ico',
      tag: options.tag,
      requireInteraction: options.requireInteraction || false,
      silent: true // 默认静音，通过 playBeep() 单独控制声音
    })

    if (options.onClick) {
      notification.onclick = (event) => {
        event.preventDefault()
        // 自动聚焦窗口
        window.focus()
        options.onClick(event, notification)
        notification.close()
      }
    }

    if (options.onClose) {
      notification.onclose = () => {
        options.onClose(notification)
      }
    }

    console.log('[BrowserAlert] 通知已显示:', options.title)
    return notification
  } catch (error) {
    console.error('[BrowserAlert] 显示通知失败:', error)
    return null
  }
}

/**
 * 播放提示音（使用 Web Audio API 生成蜂鸣声，无需外部音频文件）
 * @param {Object} [options] - 声音选项
 * @param {number} [options.frequency=800] - 频率 (Hz)
 * @param {number} [options.duration=0.15] - 持续时间 (秒)
 * @param {number} [options.volume=0.3] - 音量 (0-1)
 * @param {string} [options.type='sine'] - 波形类型：sine, square, sawtooth, triangle
 * @param {number} [options.repeat=2] - 重复次数
 * @param {number} [options.interval=0.1] - 重复间隔 (秒)
 */
export function playBeep(options = {}) {
  const {
    frequency = 800,
    duration = 0.15,
    volume = 0.3,
    type = 'sine',
    repeat = 2,
    interval = 0.1
  } = options

  try {
    // 创建音频上下文
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) {
      console.warn('[BrowserAlert] 不支持 Web Audio API')
      return
    }

    const audioContext = new AudioContextClass()
    let currentRepeat = 0

    const playOnce = () => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()

      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)

      oscillator.type = type
      oscillator.frequency.setValueAtTime(frequency, audioContext.currentTime)

      // 设置音量包络
      gainNode.gain.setValueAtTime(0, audioContext.currentTime)
      gainNode.gain.linearRampToValueAtTime(volume, audioContext.currentTime + 0.01)
      gainNode.gain.linearRampToValueAtTime(0, audioContext.currentTime + duration)

      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + duration)

      currentRepeat++

      if (currentRepeat < repeat) {
        setTimeout(playOnce, interval * 1000)
      } else {
        // 延迟关闭上下文，确保最后一个声音播放完
        setTimeout(() => {
          audioContext.close()
        }, (duration + interval) * 1000)
      }
    }

    playOnce()
    console.log('[BrowserAlert] 播放提示音')
  } catch (error) {
    console.error('[BrowserAlert] 播放提示音失败:', error)
  }
}

/**
 * 组合告警：同时显示桌面通知和播放提示音
 * @param {Object} options - 告警选项
 * @param {string} options.title - 通知标题
 * @param {string} options.body - 通知内容
 * @param {boolean} [options.soundEnabled=true] - 是否启用声音
 * @param {Object} [options.soundOptions] - 声音选项（见 playBeep）
 * @param {Object} [options.notificationOptions] - 通知选项（见 showNotification）
 */
export function alertWithSound(options = {}) {
  const {
    title,
    body,
    soundEnabled = true,
    soundOptions = {},
    notificationOptions = {}
  } = options

  // 显示桌面通知
  showNotification({
    title,
    body,
    ...notificationOptions
  })

  // 播放声音
  if (soundEnabled) {
    playBeep(soundOptions)
  }
}

/**
 * 播放自定义音频文件
 * @param {string} src - 音频文件 URL
 * @param {Object} [options] - 播放选项
 * @param {number} [options.volume=1] - 音量 (0-1)
 * @param {boolean} [options.loop=false] - 是否循环播放
 * @returns {HTMLAudioElement|null} 音频元素或 null
 */
export function playAudioFile(src, options = {}) {
  if (!src) {
    console.warn('[BrowserAlert] 音频文件 URL 不能为空')
    return null
  }

  try {
    const audio = new Audio(src)
    audio.volume = options.volume ?? 1
    audio.loop = options.loop ?? false
    
    audio.play().catch(error => {
      console.warn('[BrowserAlert] 自动播放被阻止:', error.message)
    })

    return audio
  } catch (error) {
    console.error('[BrowserAlert] 播放音频失败:', error)
    return null
  }
}
