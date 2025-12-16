<template>
  <div
    class="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-lg shadow-md w-full max-w-lg mx-auto"
  >
    <video
      ref="videoElement"
      autoplay
      playsinline
      muted
      width="640"
      height="480"
      class="border-4 border-blue-400 rounded-lg"
    ></video>

    <div class="mt-4 flex gap-4">
      <button
        @click="startCamera"
        class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition"
        :disabled="isRunning"
      >
        {{ isRunning ? "已开启" : "打开摄像头" }}
      </button>

      <button
        @click="stopCamera"
        class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
        :disabled="!isRunning"
      >
        关闭摄像头
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from "vue";
import { useFaceCapture } from "@/components/useFaceCapture";
import { getFaceWebSocketServer } from "@/services/faceWebSocketServer";

const faceWebSocketServer = getFaceWebSocketServer();
// 订阅确认事件
faceWebSocketServer.on("ack", () => {
  sending = false;
  console.log("发送end时间" + Date.now());
  console.log("收到确认，可以发送下一帧");
});
faceWebSocketServer.on("error", () => {
  sending = false;
  console.error("后端处理异常");
  stopCamera();
});
const isRunning = ref(false);
let sending = false;
/**
 * 捕获到人脸后
 * @param frameBuffer
 * @param width
 * @param height
 */
const onFaceCaptureSuccessCallback = async (buffer: ArrayBuffer) => {
  if (sending) return;
  sending = true;
  console.log("发送start时间" + Date.now());

  try {
    // 直接发送二进制数据
    faceWebSocketServer.send(buffer);
  } catch (err) {
    console.error("发送失败", err);
  } finally {
    sending = false;
  }
};
const { startFaceCapture, stopFaceCapture, loadModels, videoElement } =
  useFaceCapture(
    { videoWidth: 640, videoHeight: 480 },
    onFaceCaptureSuccessCallback
  );

const startCamera = async () => {
  try {
    await faceWebSocketServer.connect();
    if (isRunning.value) return;
    await startFaceCapture();
    isRunning.value = true;
    console.log("摄像头已开启");
  } catch (err) {
    console.error(err);
  }
};

const stopCamera = () => {
  stopFaceCapture();
  isRunning.value = false;
  console.log("摄像头已关闭");
};
onBeforeUnmount(() => {
  stopCamera();
});
onMounted(async () => {
  await loadModels();
});
</script>

<style scoped></style>
