<template>
  <div class="barcode-scanner">
    <div class="scanner-container">
      <video ref="videoRef" class="scanner-video" autoplay playsinline></video>
      <div class="scanner-overlay">
        <div class="scan-frame"></div>
        <div class="scan-hint">将条形码对准框内进行扫描</div>
      </div>
    </div>
    <div class="scanner-actions">
      <button class="btn-secondary" @click="stopScan">取消</button>
      <button class="btn-secondary" @click="toggleCamera" v-if="cameras.length > 1">
        切换摄像头 ({{ currentCameraIndex + 1 }}/{{ cameras.length }})
      </button>
    </div>
    <div v-if="error" class="scanner-error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['scan-result', 'close', 'error'])

const videoRef = ref(null)
const error = ref('')
const cameras = ref([])
const currentCameraIndex = ref(0)
let stream = null
let scanInterval = null

// 检查 BarcodeDetector API 是否可用
const barcodeDetectorSupported = 'BarcodeDetector' in window

onMounted(async () => {
  await startCamera()
})

onUnmounted(() => {
  stopScan()
})

async function startCamera() {
  try {
    // 获取摄像头列表
    const devices = await navigator.mediaDevices.enumerateDevices()
    cameras.value = devices.filter(d => d.kind === 'videoinput')

    if (cameras.value.length === 0) {
      error.value = '未检测到摄像头设备'
      emit('error', '未检测到摄像头设备')
      return
    }

    await openCamera(cameras.value[currentCameraIndex.value].deviceId)
    startScanning()

  } catch (err) {
    console.error('摄像头启动失败:', err)
    error.value = '无法访问摄像头，请检查权限设置'
    emit('error', error.value)
  }
}

async function openCamera(deviceId) {
  // 停止旧流
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
  }

  stream = await navigator.mediaDevices.getUserMedia({
    video: {
      deviceId: { exact: deviceId },
      facingMode: 'environment',
      width: { ideal: 1280 },
      height: { ideal: 720 }
    }
  })

  if (videoRef.value) {
    videoRef.value.srcObject = stream
  }
}

async function toggleCamera() {
  currentCameraIndex.value = (currentCameraIndex.value + 1) % cameras.value.length
  await openCamera(cameras.value[currentCameraIndex.value].deviceId)
}

function startScanning() {
  if (barcodeDetectorSupported) {
    scanWithBarcodeDetector()
  } else {
    error.value = '当前浏览器不支持条形码扫描，请使用图片识别功能'
    emit('error', error.value)
  }
}

// 使用 BarcodeDetector API 进行扫描
async function scanWithBarcodeDetector() {
  const detector = new BarcodeDetector({
    formats: [
      'ean_13', 'ean_8', 'upc_a', 'upc_e',
      'code_128', 'code_39', 'code_93',
      'codabar', 'itf', 'qr_code', 'data_matrix'
    ]
  })

  scanInterval = setInterval(async () => {
    if (!videoRef.value || videoRef.value.readyState < 2) return

    try {
      const barcodes = await detector.detect(videoRef.value)
      if (barcodes.length > 0) {
        stopScan()
        emit('scan-result', {
          barcode: barcodes[0].rawValue,
          format: barcodes[0].format
        })
      }
    } catch (err) {
      console.error('扫描失败:', err)
    }
  }, 300)
}

function stopScan() {
  if (scanInterval) {
    clearInterval(scanInterval)
    scanInterval = null
  }
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
    stream = null
  }
  emit('close')
}

defineExpose({ stopScan })
</script>

<style scoped>
.barcode-scanner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.scanner-container {
  position: relative;
  width: 100%;
  max-width: 400px;
  border-radius: 16px;
  overflow: hidden;
  background: #000;
}

.scanner-video {
  width: 100%;
  height: auto;
  display: block;
}

.scanner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.scan-frame {
  width: 70%;
  height: 100px;
  border: 3px solid rgba(16, 185, 129, 0.8);
  border-radius: 12px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.35);
}

.scan-hint {
  margin-top: 20px;
  color: #fff;
  font-size: 14px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
  background: rgba(0, 0, 0, 0.4);
  padding: 6px 16px;
  border-radius: 20px;
}

.scanner-actions {
  display: flex;
  gap: 12px;
}

.scanner-error {
  color: #ef4444;
  font-size: 14px;
  text-align: center;
}
</style>