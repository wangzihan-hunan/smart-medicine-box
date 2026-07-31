<template>
  <!-- 语音指令按钮组件 -->
  <div class="voice-command">
    <!-- 不支持语音时显示提示 -->
    <div v-if="!isSupported" class="voice-unsupported">
      <el-icon><WarningFilled /></el-icon>
      <span>当前浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器</span>
    </div>

    <!-- 语音按钮主体 -->
    <template v-else>
      <!-- 大号麦克风按钮 -->
      <button
        class="voice-btn"
        :class="{ listening: isListening, 'has-result': transcript }"
        @click="toggleListening"
        :disabled="disabled"
      >
        <el-icon class="voice-icon">
          <Microphone v-if="!isListening && !transcript" />
          <component :is="pulseIcon" v-else />
        </el-icon>
        <span class="voice-label">
          {{ isListening ? '正在聆听...' : transcript ? '点击重新说话' : '按住说话' }}
        </span>
      </button>

      <!-- 实时识别文字（边说边显示） -->
      <div v-if="isListening && interimTranscript" class="voice-interim">
        {{ interimTranscript }}
      </div>

      <!-- 最终识别结果 + 指令匹配 -->
      <div v-if="transcript && !isListening" class="voice-result">
        <div class="result-header">
          <el-icon><CircleCheck /></el-icon>
          <span>识别结果：</span>
        </div>
        <div class="result-text">"{{ transcript }}"</div>

        <!-- 匹配到的指令预览 -->
        <div v-if="matchedCommand" class="result-match">
          <div class="match-label">已识别指令：</div>
          <div class="match-card">
            <span class="match-action">{{ matchedCommand.actionLabel }}</span>
            <span v-if="matchedCommand.medicineName" class="match-medicine">
              → {{ matchedCommand.medicineName }}
            </span>
          </div>
          <div class="match-actions">
            <button class="btn-send" @click="$emit('send', matchedCommand)">
              <el-icon><MapLocation /></el-icon>
              确认发送
            </button>
            <button class="btn-cancel" @click="reset">
              <el-icon><Close /></el-icon>
              取消
            </button>
          </div>
        </div>

        <!-- 未能匹配到指令 — 区分两种错误类型 -->
        <div v-else class="result-no-match">
          <!-- 类型1：识别到文字但未匹配任何动作关键词 -->
          <div v-if="matchError === 'no_action'" class="match-error-card">
            <div class="error-header">
              <el-icon><WarningFilled /></el-icon>
              <span>无法识别该指令</span>
            </div>
            <p class="error-detail">您说的内容不在当前服务范围内，系统无法处理。</p>
            <div class="error-hint">
              <p>当前支持的指令类型：</p>
              <ul>
                <li><strong>出药</strong> — 如"出药 硝苯地平"、"吃阿司匹林"</li>
                <li><strong>开/关门</strong> — 如"打开药盒"、"关上"</li>
                <li><strong>检查库存</strong> — 如"看看还剩多少药"</li>
                <li><strong>找药盒</strong> — 如"药盒在哪里"、"响铃"</li>
                <li><strong>校准</strong> — 如"校准药盒"</li>
              </ul>
            </div>
            <button class="btn-retry" @click="reset">重新说话</button>
          </div>
          <!-- 类型2：匹配到出药指令但未找到该药品 -->
          <div v-else-if="matchError === 'no_medicine'" class="match-error-card">
            <div class="error-header">
              <el-icon><WarningFilled /></el-icon>
              <span>未找到该药品</span>
            </div>
            <p class="error-detail">识别到出药指令，但未在药品库中找到对应药品，请核实药品名称。</p>
            <button class="btn-retry" @click="reset">重新说话</button>
          </div>
          <!-- 兜底：未识别到任何指令 -->
          <div v-else class="match-error-card">
            <div class="error-header">
              <el-icon><WarningFilled /></el-icon>
              <span>未能识别指令</span>
            </div>
            <p class="error-detail">请尝试使用以下说法：</p>
            <ul>
              <li>"出药 硝苯地平"</li>
              <li>"打开药盒"</li>
              <li>"检查库存"</li>
              <li>"找药盒"</li>
            </ul>
            <button class="btn-retry" @click="reset">重新说话</button>
          </div>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="voice-error">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ error }}</span>
        <button class="btn-retry" @click="reset">重试</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Microphone, WarningFilled, CircleCheck, MapLocation, Close } from '@element-plus/icons-vue'
import { useSpeechRecognition } from '../composables/useSpeechRecognition'

const props = defineProps({
  /** 药品列表，用于匹配药品名称 */
  medicines: { type: Array, default: () => [] },
  /** 是否禁用 */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])

const { isListening, transcript, interimTranscript, error, isSupported, start, stop, reset } = useSpeechRecognition()

// 脉冲动画图标（简化为麦克风图标）
const pulseIcon = computed(() => Microphone)

// 匹配到的指令
const matchedCommand = ref(null)
// 匹配错误类型：'no_action' | 'no_medicine' | null
const matchError = ref(null)

// 指令动作关键词映射
const actionKeywords = {
  '出药': ['出药', '吃药', '服药', '拿药', '取药', '用'],
  '开门': ['开门', '打开', '开盖', '开仓'],
  '关门': ['关门', '关闭', '关上', '合上'],
  '检查库存': ['检查', '库存', '还剩多少', '还有多少', '查药', '看看药'],
  '找药盒': ['找药盒', '找药', '找一下', '在哪', '哪里', '响铃', '叫一下', '蜂鸣', '滴滴'],
  '校准': ['校准', '校正', '归零', '重置']
}

// 监听识别结果变化，自动匹配指令
watch(() => transcript.value, (text) => {
  if (text && !isListening.value) {
    const result = parseVoiceCommand(text, props.medicines)
    if (result) {
      matchedCommand.value = result
      matchError.value = null
    }
  } else {
    matchedCommand.value = null
    matchError.value = null
  }
})

/**
 * 解析语音文本，匹配指令和药品
 */
function parseVoiceCommand(text, medicines) {
  const lower = text.toLowerCase()

  // 1. 匹配动作
  let matchedAction = null
  let matchedActionLabel = null
  for (const [action, keywords] of Object.entries(actionKeywords)) {
    for (const kw of keywords) {
      if (lower.includes(kw)) {
        matchedAction = action
        matchedActionLabel = action
        break
      }
    }
    if (matchedAction) break
  }

  // 未匹配到任何动作关键词 → 报错
  if (!matchedAction) {
    matchError.value = 'no_action'
    return null
  }

  // 2. 匹配药品名称（仅出药指令需要）
  let matchedMedicine = null
  if (matchedAction === '出药' && medicines.length > 0) {
    for (const med of medicines) {
      if (med.name && lower.includes(med.name.toLowerCase())) {
        matchedMedicine = med
        break
      }
    }
    // 出药指令但未匹配到药品 → 报错
    if (!matchedMedicine) {
      matchError.value = 'no_medicine'
      return null
    }
  }

  // 3. 映射动作到后端 action 值
  const actionMap = {
    '出药': 'DISPENSE_MEDICINE',
    '开门': 'OPEN_DOOR',
    '关门': 'CLOSE_DOOR',
    '检查库存': 'CHECK_STOCK',
    '校准': 'CALIBRATE',
    '找药盒': 'BEEP'
  }

  return {
    action: actionMap[matchedAction] || 'DISPENSE_MEDICINE',
    actionLabel: matchedActionLabel,
    medicineId: matchedMedicine ? matchedMedicine.id : null,
    medicineName: matchedMedicine ? matchedMedicine.name : null,
    params: text
  }
}

function toggleListening() {
  if (isListening.value) {
    stop()
  } else {
    start()
  }
}

/** 重置并发送 */
function handleSend() {
  if (matchedCommand.value) {
    emit('send', matchedCommand.value)
    reset()
    matchedCommand.value = null
  }
}
</script>

<style scoped>
.voice-command {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

/* 不支持提示 */
.voice-unsupported {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #fef3c7;
  border-radius: 12px;
  color: #92400e;
  font-size: 14px;
}

/* 麦克风按钮 */
.voice-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 140px;
  height: 140px;
  border: 3px solid #e5e7eb;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 15px;
  color: #6b7280;
}

.voice-btn:hover {
  border-color: #10b981;
  box-shadow: 0 0 24px rgba(16, 185, 129, 0.15);
}

.voice-btn.listening {
  border-color: #ef4444;
  background: #fef2f2;
  animation: pulse 1.5s ease-in-out infinite;
}

.voice-btn.has-result {
  border-color: #10b981;
  background: #ecfdf5;
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-icon {
  font-size: 40px;
  color: #10b981;
}

.listening .voice-icon {
  color: #ef4444;
}

.voice-label {
  font-weight: 500;
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.3); }
  50% { box-shadow: 0 0 0 16px rgba(239, 68, 68, 0); }
}

/* 实时文字 */
.voice-interim {
  font-size: 16px;
  color: #9ca3af;
  font-style: italic;
  min-height: 24px;
  padding: 0 16px;
  text-align: center;
}

/* 识别结果区域 */
.voice-result {
  width: 100%;
  max-width: 400px;
  padding: 20px;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 16px;
  text-align: center;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #16a34a;
  font-weight: 600;
  margin-bottom: 8px;
}

.result-text {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 16px;
  padding: 8px 16px;
  background: white;
  border-radius: 8px;
}

/* 匹配结果 */
.result-match {
  margin-top: 12px;
}

.match-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.match-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: #dbeafe;
  border-radius: 10px;
  margin-bottom: 16px;
}

.match-action {
  font-weight: 700;
  color: #1d4ed8;
  font-size: 16px;
}

.match-medicine {
  color: #6b7280;
  font-size: 14px;
}

.match-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 未匹配 */
.result-no-match {
  margin-top: 12px;
}

.match-error-card {
  padding: 16px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 12px;
  text-align: center;
}

.match-error-card .error-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #d97706;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 8px;
}

.match-error-card .error-detail {
  font-size: 14px;
  color: #92400e;
  margin-bottom: 12px;
  line-height: 1.5;
}

.match-error-card .error-hint {
  text-align: left;
  padding: 12px;
  background: #fffbeb;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #78716c;
  border: 1px dashed #fcd34d;
}

.match-error-card .error-hint p {
  margin: 0 0 6px;
  font-weight: 600;
  color: #78716c;
}

.match-error-card .error-hint ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.match-error-card .error-hint li {
  padding: 3px 0;
  color: #a8a29e;
  font-size: 12px;
}

.match-error-card .error-hint li strong {
  color: #78716c;
}

.match-error-card ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 16px;
}

.match-error-card li {
  padding: 4px 0;
  color: #9ca3af;
  font-size: 13px;
}

/* 按钮 */
.btn-send, .btn-retry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-send:hover, .btn-retry:hover {
  background: #059669;
}

.btn-cancel {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

/* 错误提示 */
.voice-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fef2f2;
  border-radius: 10px;
  color: #dc2626;
  font-size: 14px;
}
</style>