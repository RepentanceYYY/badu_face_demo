import { onBeforeUnmount, ref } from "vue";
import * as faceapi from "face-api.js";

interface FaceCaptureOptions {
  videoWidth?: number;
  videoHeight?: number;
}

/**
 * 捕获人脸帧回调
 * frameBuffer: 原始 RGBA 数据
 * width/height: 视频帧分辨率
 */
type FaceCaptureSuccessCallback = (buffer: ArrayBuffer) => void;

export const useFaceCapture = (
  options: FaceCaptureOptions = {},
  onFaceCaptureSuccess?: FaceCaptureSuccessCallback
) => {
  const videoElement = ref<HTMLVideoElement | null>(null);
  const faceCaptureStatus = ref<
    "waiting" | "detecting" | "capturing" | "error"
  >("waiting");
  let stream: MediaStream | null = null;
  let detecting = false;
  let rafId: number | null = null;
  const VIDEO_WIDTH = options.videoWidth || 1280;
  const VIDEO_HEIGHT = options.videoHeight || 720;
  const sending = ref
  let offscreenCanvas: HTMLCanvasElement | null = null;
  let offscreenCtx: CanvasRenderingContext2D | null = null;

  // 加载 face-api.js 模型
  const loadModels = async () => {
    await faceapi.nets.tinyFaceDetector.loadFromUri("/face/model");
  };

  // 启动摄像头和检测
  const startFaceCapture = async () => {
    faceCaptureStatus.value = "waiting";
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { width: VIDEO_WIDTH, height: VIDEO_HEIGHT },
      });
      if (videoElement.value) videoElement.value.srcObject = stream;
      // 等待视频可以播放
      await new Promise<void>((resolve) => {
        videoElement.value?.addEventListener(
          "canplay",
          () => {
            videoElement.value?.play();
            resolve();
          },
          { once: true }
        );
      });
      detecting = true;
      faceCaptureStatus.value = "detecting";
      rafId = requestAnimationFrame(detectFaceLoop);
    } catch (err) {
      console.error(err);
      faceCaptureStatus.value = "error";
    }
  };

  // 停止摄像头
  const stopFaceCapture = () => {
    detecting = false; // 先停止循环
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (stream) {
      stream.getTracks().forEach((t) => t.stop());
      stream = null;
    }
    if (videoElement.value) videoElement.value.srcObject = null;
  };

  // 主循环：检测到人脸就截帧
  const detectFaceLoop = async () => {
    if (!videoElement.value || !detecting || !stream) return;
    if (isProcessingBlob) {
      rafId = requestAnimationFrame(detectFaceLoop);
      return;
    }

    const options = new faceapi.TinyFaceDetectorOptions();
    const detection = await faceapi.detectSingleFace(
      videoElement.value,
      options
    );

    if (detection) {
      captureFrame();
    }

    // 稍微延迟下一帧检测，没必要每秒检测 60 次
    setTimeout(() => {
      if (detecting) rafId = requestAnimationFrame(detectFaceLoop);
    }, 100); // 100ms 间隔
  };
  // 定义一个内部锁，防止 toBlob 还在进行中时又触发绘图
  let isProcessingBlob = false;
  // 截取视频帧并发送原始 RGBA 数据
  const captureFrame = () => {
    const video = videoElement.value;
    if (!video || video.readyState < 2 || isProcessingBlob) return;

    if (!offscreenCanvas) {
      offscreenCanvas = document.createElement("canvas");
      offscreenCanvas.width = video.videoWidth;
      offscreenCanvas.height = video.videoHeight;
      offscreenCtx = offscreenCanvas.getContext("2d", {
        willReadFrequently: true,
      })!;
    }

    const ctx = offscreenCtx!;
    ctx.drawImage(video, 0, 0, offscreenCanvas.width, offscreenCanvas.height);

    isProcessingBlob = true; // 上锁

    offscreenCanvas.toBlob(
      (blob) => {
        if (!blob) {
          isProcessingBlob = false;
          return;
        }

        const reader = new FileReader();
        reader.onload = () => {
          const arrayBuffer = reader.result as ArrayBuffer;
          if (onFaceCaptureSuccess) {
            onFaceCaptureSuccess(arrayBuffer);
          }
          isProcessingBlob = false; // 只有在完全转成 Buffer 后才解锁
        };
        reader.onerror = () => {
          isProcessingBlob = false;
        };
        reader.readAsArrayBuffer(blob);
      },
      "image/jpeg",
      0.9
    ); // 【建议】0.9 是性价比最高点，1.0 容易产生硬件编码瓶颈
  };

  onBeforeUnmount(() => {
    stopFaceCapture();
  });

  return {
    videoElement,
    faceCaptureStatus,
    startFaceCapture,
    stopFaceCapture,
    loadModels,
  };
};
