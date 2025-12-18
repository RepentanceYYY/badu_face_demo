<template>
    <div class="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 flex items-center justify-center p-4">
        <div class="bg-white rounded-xl shadow-lg p-8 max-w-md w-full">
            <!-- 验证区域 -->
            <div class="bg-gray-50 rounded-xl p-6 mb-8" :class="{ 'animate-pulse': isVerifying }">
                <div class="text-center">
                    <!-- 双目摄像头视图 -->
                    <div class="relative bg-gray-900 rounded-xl overflow-hidden h-64 mb-4">
                        <!-- 摄像头画面 -->
                        <video v-show="!faceImage && faceStatus !== 'waiting' && faceStatus !== 'error'"
                            ref="videoElement" class="w-full h-full object-cover" autoplay playsinline></video>

                        <!-- 采集结果预览 -->
                        <div v-if="faceImage" class="absolute inset-0 flex items-center justify-center bg-black">
                            <img :src="faceImage" alt="采集的人脸照片" class="max-h-full max-w-full object-contain" />
                        </div>

                        <!-- 准备拍摄提示 -->
                        <div v-if="faceStatus === 'waiting' || faceStatus === 'error'"
                            class="absolute inset-0 flex flex-col items-center justify-center p-4 text-center bg-gray-800 text-gray-300">
                            <Camera class="w-10 h-10 mb-3" />
                            <p class="text-sm mb-2">请点击开始{{ faceStatus === 'waiting' ? '开始验证' : '重新验证' }}按钮</p>
                            <p class="text-xs">系统将自动检测并验证人脸</p>
                        </div>

                        <!-- 人脸检测框 -->
                        <div v-if="detectionBox.visible && !faceImage"
                            class="absolute border-2 border-green-400 rounded-md" :style="{
                                left: `${detectionBox.x}%`,
                                top: `${detectionBox.y}%`,
                                width: `${detectionBox.width}%`,
                                height: `${detectionBox.height}%`,
                            }"></div>

                        <!-- 质量提示 -->
                        <div v-if="qualityHint && !faceImage"
                            class="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-black/70 text-white px-3 py-1 rounded-full text-sm flex items-center">
                            <Info class="w-4 h-4 mr-1" />
                            {{ qualityHint }}
                        </div>
                    </div>

                    <p class="text-sm font-medium text-gray-700">{{ faceStatusText }}</p>
                    <p class="text-xs text-gray-500 mt-1">确保光线充足，面部正对摄像头</p>

                    <button v-if="faceStatus === 'waiting' || faceStatus === 'error'" @click="startFaceVerification"
                        class="mt-4 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors">
                        {{ faceStatus === 'waiting' ? '开始验证' : '重新验证' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
const faceImage = ref<string>('');
const faceStatus = ref<string>('waiting');
const faceStatusText = ref<string>('');
const isVerifying = ref(false);
/**
   * 人脸检测模拟
   */
const detectionBox = ref({
    visible: false,
    x: 30,
    y: 30,
    width: 40,
    height: 40,
});
/**
 * 质量提示
 */
const qualityHint = ref("");
const startFaceVerification = () => {

}
</script>