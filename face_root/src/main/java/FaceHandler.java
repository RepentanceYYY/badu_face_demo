import com.alibaba.fastjson2.JSONObject;
import com.jni.face.Face;
import com.jni.struct.EyeClose;
import com.jni.struct.LivenessInfo;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class FaceHandler {
    static String path = "C:\\Users\\Administrator\\Desktop\\test_tmp";

    public static FaceLivenessResult bestFace(JSONObject obj) throws IOException {
        String base64 = obj.getString("data");
        Mat rgbMat = null;
        FaceLivenessResult faceLivenessResult = new FaceLivenessResult();
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            MatOfByte matOfByte = new MatOfByte(bytes);
            rgbMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

            if (rgbMat.empty()) {
                throw new RuntimeException("检测不到人脸");
            }

            long rgbMatAddr = rgbMat.getNativeObjAddr();
            LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
            if (liveInfos == null || liveInfos.length <= 0 || liveInfos[0].box == null) {
                throw new RuntimeException("检测不到人脸");
            }
            float liveScore = liveInfos[0].livescore;
            float[] blurList = Face.faceBlur(rgbMatAddr);
            if (blurList == null || blurList.length <= 0) {
                throw new RuntimeException("检测不到人脸，来源于模糊度检测");
            }
            EyeClose[] eyeCloses = Face.faceEyeClose(rgbMatAddr);
            if (eyeCloses == null || eyeCloses.length < 1) {
                throw new RuntimeException("检测不到人脸，来源于眼睛闭合检测");
            }
            float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
            System.out.println(blurList[0]);
            if (blurList[0] > 0.01f) {
                System.out.println(String.format("清晰度太低，%.3f", blurList[0]));
            } else {
                faceLivenessResult.setSharp(true);
            }
            if (liveScore < 0.6f) {
                System.out.println(String.format("检测到非活体,%.3f", liveScore));
            } else {
                faceLivenessResult.setLive(true);
            }
            if (eyeCloses[0].leftEyeCloseConf > 0.1f || eyeCloses[0].rightEyeCloseConf > 0.1f) {
                System.out.println("检测到闭眼");
                faceLivenessResult.setEyeClosed(true);
            }
            if (mouthCloseScore[0] < 0.9f) {
                System.out.println("检测到张嘴");
                faceLivenessResult.setMouthOpen(true);
            }
            // 如果清晰度高，并静默检测为活体，并且判断为未闭眼则保存图片
            if (faceLivenessResult.isSharp() && faceLivenessResult.isLive() && !faceLivenessResult.isEyeClosed() && !faceLivenessResult.isMouthOpen()) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                File dir = new File(path);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, System.currentTimeMillis() + ".jpg");
                ImageIO.write(img, "jpg", file);
                System.out.println("输出照片");
            }
            faceLivenessResult.setLive(liveInfos[0].livescore > 0.8f);
            faceLivenessResult.setEyeClosed(eyeCloses[0].leftEyeCloseConf > 0.05f || eyeCloses[0].rightEyeCloseConf > 0.05f);
            faceLivenessResult.setMouthOpen(mouthCloseScore[0] < 0.9f);
            return faceLivenessResult;
        } catch (Exception e) {
            throw e;
        } finally {
            if (rgbMat != null) rgbMat.release();
        }
    }
}
