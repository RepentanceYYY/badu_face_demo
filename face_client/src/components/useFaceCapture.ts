import { onBeforeUnmount, ref, type Ref } from "vue";
import * as faceapi from "face-api.js";

interface FaceCaptureOptions {
  videoWidth?: number;
  videoHeight?: number;
}

/**
 * 捕获人脸帧回调
 * arrayBuffer: 原始 JPEG 数据（ArrayBuffer）
 */
type FaceCaptureSuccessCallback = (base64: string) => void;

export const useFaceCapture = (
  options: FaceCaptureOptions = {},
  onFaceCaptureSuccess?: FaceCaptureSuccessCallback,
  hintMessage?: Ref<string>
) => {
  const videoElement = ref<HTMLVideoElement | null>(null);
  const faceCaptureStatus = ref<
    "waiting" | "detecting" | "capturing" | "error"
  >("waiting");

  const externalLock = ref(false); // 外部锁，防止并发捕获

  let isProcessingBlob = false; // 内部锁，防止 toBlob 还没完成又开始新一轮绘制

  let stream: MediaStream | null = null;
  let detecting = false;
  let rafId: number | null = null;

  let offscreenCanvas: HTMLCanvasElement | null = null;
  let offscreenCtx: CanvasRenderingContext2D | null = null;

  let prevFaceCenter: { x: number; y: number } | null = null;
  const headMoveThreshold = 10; // 头部可移动像素阈值



  // 加载 face-api.js 模型
  const loadModels = async () => {
    await faceapi.nets.tinyFaceDetector.loadFromUri("/face/model");
    await faceapi.nets.faceLandmark68Net.loadFromUri("/face/model");
  };
  const openWebcams = async () => {
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { min: 1280, ideal: 1920 },
          height: { min: 720, ideal: 1080 },
          frameRate: { ideal: 30 },
        },
      });

      if (videoElement.value) {
        videoElement.value.srcObject = stream;
      }
    } catch (error) {
      throw new Error('摄像头访问失败');
    }
  }

  // 启动摄像头和人脸检测
  const startFaceCapture = async () => {
    faceCaptureStatus.value = "waiting";
    try {
      await openWebcams();
      await new Promise<void>((resolve) => {
        videoElement.value?.addEventListener(
          "canplaythrough",
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
    } catch (error) {
      throw error;
    }
  };

  // 停止摄像头和检测
  const stopFaceCapture = () => {
    detecting = false;
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (stream) {
      stream.getTracks().forEach((t) => t.stop());
      stream = null;
    }
    if (videoElement.value) {
      videoElement.value.srcObject = null;
    }
    faceCaptureStatus.value = "waiting";
  };

  // 主检测循环
  const detectFaceLoop = async () => {
    if (!videoElement.value || !detecting || !stream) return;

    const video = videoElement.value;

    // 第一次进入检测时加个短暂延迟，确保视频流稳定出图
    if (faceCaptureStatus.value === "detecting") {
      // 状态刚变成 detecting，说明是第一次循环
      // 等待一小会儿让画面稳定
      await new Promise((r) => setTimeout(r, 200));
    }

    if (externalLock.value || isProcessingBlob) {
      // 被锁住，稍后再试
      setTimeout(() => {
        if (detecting) rafId = requestAnimationFrame(detectFaceLoop);
      }, 120);
      return;
    }

    const options = new faceapi.TinyFaceDetectorOptions();
    const detection = await faceapi.detectSingleFace(video, options).withFaceLandmarks();
    let isHeadMoving = false;
    if (detection) {
      // 获取关键点：鼻子 + 左眼 + 右眼
      const keypoints = [
        ...detection.landmarks.getNose(),
        ...detection.landmarks.getLeftEye(),
        ...detection.landmarks.getRightEye(),
      ];
      // 计算质心
      const avgX = keypoints.reduce((sum, p) => sum + p.x, 0) / keypoints.length;
      const avgY = keypoints.reduce((sum, p) => sum + p.y, 0) / keypoints.length;
      if (prevFaceCenter) {
        const delta = Math.hypot(avgX - prevFaceCenter.x, avgY - prevFaceCenter.y);
        isHeadMoving = delta > headMoveThreshold;
      }
      prevFaceCenter = { x: avgX, y: avgY };
      if (!isHeadMoving) {
        captureFrame();
      } else {
        if (hintMessage) {
          hintMessage.value = "请勿晃动";
        }
      }
    }

    // 继续下一轮检测（约8-10fps检测，足够且不卡）
    setTimeout(() => {
      if (detecting) rafId = requestAnimationFrame(detectFaceLoop);
    }, 120);
  };
  /**
   * 截取当前视频帧并转为 ArrayBuffer 发送
   * @returns base64
   */
  const captureFrame = () => {
    const video = videoElement.value;
    if (!video) return;

    // 严格检查视频是否真正就绪
    if (
      video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA || // < 2
      video.videoWidth === 0 ||
      video.videoHeight === 0 ||
      isProcessingBlob ||
      externalLock.value
    ) {
      console.warn("视频帧尚未就绪，跳过本次捕获");
      return;
    }

    // 初始化离屏 canvas
    if (!offscreenCanvas) {
      offscreenCanvas = document.createElement("canvas");
      offscreenCanvas.width = video.videoWidth;
      offscreenCanvas.height = video.videoHeight;
      offscreenCtx = offscreenCanvas.getContext("2d", {
        alpha: false,
        desynchronized: false,
        willReadFrequently: true,
      })!;

      if (!offscreenCtx) {
        console.error("无法获取 2D 上下文");
        return;
      }
    }

    const ctx = offscreenCtx!;
    ctx.clearRect(0, 0, offscreenCanvas.width, offscreenCanvas.height);
    ctx.drawImage(video, 0, 0, offscreenCanvas.width, offscreenCanvas.height);

    isProcessingBlob = true;

    const base64 = offscreenCanvas.toDataURL("image/jpeg", 1.0).split(',')[1];

    if (!base64 || base64.length < 1000) {
      console.warn("生成的 base64 太短，疑似无效帧");
      isProcessingBlob = false;
      return;
    }

    // console.log(`人脸帧捕获成功,base64大小≈: ${(base64.length * 0.75 / 1024).toFixed(1)} KB `);

    if (onFaceCaptureSuccess) {
      onFaceCaptureSuccess(base64);
    }

    isProcessingBlob = false;
  };

  onBeforeUnmount(() => {
    stopFaceCapture();
  });

  return {
    videoElement,
    faceCaptureStatus,
    externalLock,
    loadModels,
    startFaceCapture,
    stopFaceCapture,
  };
};