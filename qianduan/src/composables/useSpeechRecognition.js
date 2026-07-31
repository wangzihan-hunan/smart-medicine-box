import { ref, onUnmounted } from 'vue'

/**
 * 语音识别 Composable
 * 封装浏览器 Web Speech API，支持中文语音识别
 *
 * 使用方式：
 *   const { isListening, transcript, start, stop, isSupported } = useSpeechRecognition()
 *
 * 浏览器兼容性：Chrome 25+, Edge 79+, Safari 14.1+
 * 注意：需要 HTTPS 或 localhost 环境，且用户需授权麦克风权限
 */
export function useSpeechRecognition() {
  // 是否正在监听
  const isListening = ref(false)
  // 识别结果文本（最终结果）
  const transcript = ref('')
  // 实时中间结果（边说边显示）
  const interimTranscript = ref('')
  // 错误信息
  const error = ref(null)
  // 浏览器是否支持
  const isSupported = ref(false)

  let recognition = null
  // 追踪底层识别器是否已调用 start() 且尚未触发 onend/onerror
  // 与 isListening（UI 状态）分离，解决 stop() 异步导致的竞态窗口
  let _recognitionStarted = false

  // 初始化 SpeechRecognition
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (SpeechRecognition) {
    isSupported.value = true
    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'              // 中文识别
    recognition.interimResults = true        // 开启中间结果，实现边说边显示
    recognition.continuous = false           // 单次识别，说完自动停止
    recognition.maxAlternatives = 1          // 只取最佳结果

    recognition.onresult = (event) => {
      let final = ''
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          final += result[0].transcript
        } else {
          interim += result[0].transcript
        }
      }
      if (final) {
        transcript.value = final
        interimTranscript.value = ''
      } else {
        interimTranscript.value = interim
      }
    }

    recognition.onerror = (event) => {
      _recognitionStarted = false
      // 'no-speech' 表示没检测到语音，不算真正的错误
      if (event.error !== 'no-speech') {
        error.value = getErrorMessage(event.error)
      }
      isListening.value = false
    }

    recognition.onend = () => {
      _recognitionStarted = false
      // 如果用户没有手动停止，且拿到了结果，保持 listening 状态让 UI 显示结果
      if (!transcript.value) {
        isListening.value = false
      }
    }
  }

  /**
   * 开始监听
   */
  function start() {
    if (!recognition || _recognitionStarted) return
    error.value = null
    transcript.value = ''
    interimTranscript.value = ''
    isListening.value = true
    try {
      _recognitionStarted = true
      recognition.start()
    } catch (e) {
      console.error('Speech recognition start failed:', e)
      _recognitionStarted = false
      isListening.value = false
    }
  }

  /**
   * 停止监听（使用 abort 立即终止，避免 stop 异步竞态）
   */
  function stop() {
    if (!recognition) return
    isListening.value = false
    if (_recognitionStarted) {
      recognition.abort()
      _recognitionStarted = false
    }
  }

  /**
   * 重置状态（清除上次结果）
   */
  function reset() {
    if (recognition && _recognitionStarted) {
      recognition.abort()
      _recognitionStarted = false
    }
    transcript.value = ''
    interimTranscript.value = ''
    error.value = null
    isListening.value = false
  }

  onUnmounted(() => {
    if (recognition) {
      recognition.abort()
    }
  })

  return { isListening, transcript, interimTranscript, error, isSupported, start, stop, reset }
}

/**
 * 将 Web Speech API 错误码转为中文提示
 */
function getErrorMessage(code) {
  const messages = {
    'not-allowed': '麦克风权限被拒绝，请在浏览器设置中允许麦克风访问',
    'audio-capture': '未找到麦克风设备',
    'network': '网络连接失败，请检查网络',
    'aborted': '语音识别已取消',
    'service-not-allowed': '浏览器不支持语音识别',
    'bad-grammar': '语音识别语法错误',
    'language-not-supported': '当前浏览器不支持中文语音识别'
  }
  return messages[code] || `语音识别出错（${code}）`
}