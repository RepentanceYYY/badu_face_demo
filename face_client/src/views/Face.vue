<template>
    <div class="max-w-2xl mx-auto">
        <!-- 人脸采集模块 -->
        <div class="bg-white rounded-xl border border-gray-200 p-5 mt-10">
            <div class="flex items-center justify-between mb-4">
                <div class="flex items-center">
                    <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center mr-3">
                        <Camera class="w-5 h-5 text-blue-600" />
                    </div>
                    <div>
                        <h5 class="font-medium text-gray-800">人脸采集</h5>
                        <p class="text-xs text-gray-500">用于人脸识别验证</p>
                    </div>
                </div>
                <span class="px-3 py-1 text-xs rounded-full" :class="getflowStatusClass()">
                    {{ getflowStatusText() }}
                </span>
            </div>

            <!-- 摄像头区域 -->
            <div class="relative bg-gray-900 rounded-lg overflow-hidden h-64 mb-4">
                <!-- 摄像头画面 -->
                <video v-show="shouldShowVideo" ref="videoElement" class="w-full h-full object-contain" autoplay
                    playsinline></video>

                <!-- 采集结果预览 -->
                <div v-if="shouldShowPreview" class="absolute inset-0 flex items-center justify-center bg-black">
                    <img :src="previewImage" alt="采集的人脸照片" class="max-h-full max-w-full object-contain" />
                </div>

                <!-- 状态提示 -->
                <div v-if="showStatusOverlay" class="absolute top-3 left-0 right-0 flex justify-center">
                    <div class="bg-black/70 text-white px-4 py-2 rounded-full text-sm backdrop-blur-sm">
                        <template v-if="flowStatus === 'starting'">
                            <span class="flex items-center">
                                <div class="animate-spin rounded-full h-3 w-3 border-b-2 border-white mr-2"></div>
                                启动中...
                            </span>
                        </template>
                        <template v-else-if="flowStatus === 'detecting'">
                            <span class="flex items-center">
                                <User class="w-4 h-4 mr-2 text-yellow-400" />
                                {{ hintMessage }}
                            </span>
                        </template>
                        <template v-else-if="flowStatus === 'valid'">
                            <span class="flex items-center">
                                <CheckCircle class="w-4 h-4 mr-2 text-green-400" />
                                人脸可用
                            </span>
                        </template>
                        <template v-else-if="flowStatus === 'error'">
                            <span class="flex items-center">
                                <AlertTriangle class="w-4 h-4 mr-2 text-red-400 animate-pulse" />
                                {{ errorMessage }}
                            </span>
                        </template>
                        <template v-else-if="flowStatus === 'waiting'">
                            <span class="flex items-center">
                                <Camera class="w-4 h-4 mr-2 text-gray-400" />
                                点击下方按钮开始采集
                            </span>
                        </template>
                    </div>
                </div>

                <!-- 倒计时进度条 -->
                <div v-if="flowStatus === 'detecting'" class="absolute bottom-3 left-4 right-4">
                    <div class="h-2 w-full bg-gray-700 rounded-full overflow-hidden">
                        <div class="h-full bg-yellow-400 transition-all duration-1000 ease-linear"
                            :style="{ width: (faceDetectionCountdown / 30) * 100 + '%' }"></div>
                    </div>
                    <div class="text-center mt-2">
                        <span class="text-white text-xs bg-black/50 px-3 py-1 rounded-full">
                            {{ faceDetectionCountdown }}秒后自动关闭
                        </span>
                    </div>
                </div>
            </div>

            <!-- 采集指引 -->
            <div class="mb-6 bg-blue-50 rounded-lg p-4 border border-blue-200">
                <h6 class="text-sm font-medium text-blue-800 mb-2 flex items-center">
                    <Info class="w-4 h-4 mr-2" />
                    采集指引
                </h6>
                <ul class="text-xs text-blue-700 space-y-1">
                    <li class="flex items-start">
                        <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1 mr-2 flex-shrink-0"></div>
                        请正对摄像头，保持面部光线充足
                    </li>
                    <li class="flex items-start">
                        <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1 mr-2 flex-shrink-0"></div>
                        确保整个面部在画面中央
                    </li>
                    <li class="flex items-start">
                        <div class="w-1.5 h-1.5 rounded-full bg-orange-500 mt-1 mr-2 flex-shrink-0"></div>
                        <span class="font-medium">保持静止2-3秒</span>以获得清晰图像
                    </li>
                    <li class="flex items-start">
                        <div class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1 mr-2 flex-shrink-0"></div>
                        避免戴帽子、口罩等遮挡物
                    </li>
                </ul>
            </div>

            <!-- 操作按钮 -->
            <div class="flex justify-between">
                <div class="flex space-x-3">
                    <button @click="stopFaceCapture"
                        class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center"
                        :disabled="isAnyBiometricCollecting">
                        <ChevronLeft class="w-4 h-4 mr-2" />
                        返回
                    </button>
                    <button v-if="showRetryButton" @click="handleRetryFaceCapture"
                        class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center"
                        :disabled="isAnyBiometricCollecting">
                        <RefreshCw class="w-4 h-4 mr-2" />
                        重新采集
                    </button>
                </div>
                <button v-if="showStartButton" @click="handleStartFaceCapture"
                    class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors flex items-center shadow-md"
                    :disabled="isAnyBiometricCollecting">
                    <Camera class="w-4 h-4 mr-2" />
                    开始人脸采集
                </button>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Camera, User, CheckCircle, UserX, AlertTriangle, Info, ChevronLeft, RefreshCw } from 'lucide-vue-next'
import { getFaceWebSocketServer } from '@/services/faceWebSocketServer';
import { useFaceCapture } from "@/components/useFaceCapture";
const flowStatus = ref<'waiting' | 'starting' | 'detecting' | 'valid' | 'error'>('waiting');
const activeTab = ref('');
const errorMessage = ref('');
const showStatusOverlay = ref(true);
const faceWebSocketServer = getFaceWebSocketServer();
const hintMessage = ref('');
const actionCount = ref(0);
const oldFaceURL = ref('');
let frameTmp = '';
const frameFinally = ref('');
faceWebSocketServer.on('capture', (message: any) => {
    console.log('收到回复消息时间戳：' + Date.now());
    if (message?.errorMessage) {
        hintMessage.value = message.errorMessage;
        flowStatus.value = 'error';
        stopFaceCapture();
        externalLock.value = false;
        return;
    }
    if (message?.successMessage) {
        frameFinally.value = `data:image/jpeg;base64,${frameTmp}`;
        flowStatus.value = "valid";
        stopFaceCapture();
        externalLock.value = false;
        return;
    }
    if (message?.hintMessage) {
        hintMessage.value = message?.hintMessage;
        flowStatus.value = 'detecting';
    }
    externalLock.value = false;
})
/**
 * 启动
 */
const handleStartFaceCapture = async () => {
    flowStatus.value = 'starting';
    try {
        await startFaceCapture().then(() => {
            hintMessage.value = "面向摄像头";
            flowStatus.value = 'detecting';
            //startFaceDetectionTimeout();
        });
    } catch (error: any) {
        stopFaceCapture();
        errorMessage.value = error.message;
        flowStatus.value = 'error';
    }
}
const handleRetryFaceCapture = async () => {
    frameFinally.value = '';
    await handleStartFaceCapture();
}
const getflowStatusClass = () => {

}
const getflowStatusText = () => {
    
}
const handleFaceCaptureSuccessCallback = (base64: string) => {
    if (externalLock.value) return;
    externalLock.value = true;
    try {
        console.log('发送消息时间戳:' + Date.now());
        frameTmp = base64;
        const message = JSON.stringify({ type: "capture", frame: base64, userName: null, action: null });
        faceWebSocketServer.sendWithAck(message);
    } catch (err) {
        console.error("发送失败", err);
    } finally {

    }
}
const showStartButton = computed(() => {
    return ['waiting','error','valid'].includes(flowStatus.value) && (!oldFaceURL.value && !frameFinally.value);
})
const showRetryButton = computed(() => {    
    return ['waiting','error','valid'].includes(flowStatus.value) && (oldFaceURL.value || frameFinally.value);
})
const isAnyBiometricCollecting = computed(() => {
    return false;
})
const shouldShowVideo = computed(() => {
    return flowStatus.value === 'detecting';
})
const shouldShowPreview = computed(() => {
    return flowStatus.value === 'valid' && frameFinally.value;
});
const previewImage = computed(() => {
    return frameFinally.value;
})
const faceDetectionTimeout = ref<NodeJS.Timeout | null>(null);
const faceDetectionInterval = ref<NodeJS.Timeout | null>(null);
const FACE_DETECTION_TIMEOUT = 30000;
const faceDetectionCountdown = ref(30);
// 启动人脸检测超时计时器
const startFaceDetectionTimeout = () => {
    clearFaceDetectionTimeout();

    faceDetectionCountdown.value = 30;

    faceDetectionTimeout.value = setTimeout(() => {
        if (flowStatus.value !== 'valid') {
            handleFaceDetectionTimeout();
        }
    }, FACE_DETECTION_TIMEOUT);

    faceDetectionInterval.value = setInterval(() => {
        faceDetectionCountdown.value -= 1;
        if (faceDetectionCountdown.value <= 0) {
            clearFaceDetectionTimeout();
        }
    }, 1000);
};
// 清除人脸检测超时计时器
const clearFaceDetectionTimeout = () => {
    if (faceDetectionTimeout.value) {
        clearTimeout(faceDetectionTimeout.value);
        faceDetectionTimeout.value = null;
    }
    if (faceDetectionInterval.value) {
        clearInterval(faceDetectionInterval.value);
        faceDetectionInterval.value = null;
    }
    faceDetectionCountdown.value = 30;
};
// 处理人脸检测超时
const handleFaceDetectionTimeout = () => {

    stopFaceCapture();
    flowStatus.value = 'waiting';
    frameFinally.value = '';
    clearFaceDetectionTimeout();
};
const { startFaceCapture, stopFaceCapture, externalLock, loadModels, videoElement } =
    useFaceCapture(
        { videoWidth: 640, videoHeight: 480 },
        handleFaceCaptureSuccessCallback, hintMessage
    );
onMounted(async () => {
    await loadModels();
    faceWebSocketServer.connect();
})
onBeforeUnmount(() => {
    faceWebSocketServer.close();
})
</script>