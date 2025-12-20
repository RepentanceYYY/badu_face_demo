package handler;

import com.alibaba.fastjson2.JSONObject;
import com.jni.face.Face;
import com.jni.struct.EyeClose;
import com.jni.struct.LivenessInfo;
import constants.SystemConstant;
import entity.Reply;
import entity.baidu.FaceRecognitionResponse;
import entity.baidu.FaceRecognitionResult;
import entity.db.User;
import entity.reply.FaceLivenessResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.util.StringUtils;
import server.UserService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class FaceHandler {
    static String FACE_PATH = "C:\\Users\\Administrator\\Desktop\\test_tmp";

    public static Reply capture(JSONObject obj) {
        String base64Frame = obj.getString("frame");
        Object userNameObject = obj.get("userName");
        String userName = null;
        if (userNameObject != null) {
            userName = userNameObject.toString();
        }
        Mat rgbMat = null;
        Reply reply = new Reply();
        reply.setType("capture");
        FaceLivenessResult faceLivenessResult = new FaceLivenessResult();
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Frame);
            MatOfByte matOfByte = new MatOfByte(bytes);
            rgbMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

            if (rgbMat.empty()) {
                reply.setErrorMessage("消息格式错误");
                return reply;
            }

            long rgbMatAddr = rgbMat.getNativeObjAddr();
            // 静默活体检测
            LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
            if (liveInfos == null || liveInfos.length <= 0 || liveInfos[0].box == null) {
                reply.setErrorMessage("未检测到人脸");
                return reply;
            }
            float liveScore = liveInfos[0].livescore;
            if (liveScore < 0.6f) {
                System.out.println(String.format("检测到非活体,%.3f", liveScore));
                reply.setHintMessage("未检测到人脸");
                return reply;
            }
            // 既然检测到了是活体，代表检测到了人脸

            // 人脸可用性检测
            Reply availableReply = availableDetection(rgbMatAddr, userName, "capture");
            if (availableReply != null) {
                return availableReply;
            }

            // 眼睛闭合检测
            EyeClose[] eyeCloses = Face.faceEyeClose(rgbMatAddr);
            if (eyeCloses == null || eyeCloses.length < 1) {
                reply.setHintMessage("请睁开眼睛");
                return reply;
            }
            if (eyeCloses[0].leftEyeCloseConf > 0.1f || eyeCloses[0].rightEyeCloseConf > 0.1f) {
                reply.setHintMessage("请睁开眼睛");
                return reply;
            }

            // 嘴巴闭合检测
            float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
            if (mouthCloseScore[0] < 0.9f) {
                reply.setHintMessage("检测到张嘴");
                return reply;
            }

            // 人脸模糊度检测
            float[] blurList = Face.faceBlur(rgbMatAddr);
            if (blurList == null || blurList.length <= 0) {
                reply.setHintMessage("检测不到人脸");
                return reply;
            }
            if (blurList[0] > 0.02f) {
                System.out.println(String.format("模糊度太高，%.3f", blurList[0]));
                reply.setHintMessage("人脸太模糊");
                return reply;
            }
            reply.setSuccessMessage("人脸可用");
            return reply;
        } catch (Exception e) {
            Reply errorReply = new Reply();
            errorReply.setErrorMessage(e.getMessage());
            e.printStackTrace();
            return errorReply;
        } finally {
            if (rgbMat != null) rgbMat.release();
        }
    }

    public static FaceRecognitionResponse faceRecognition(JSONObject obj) {
        Face.loadDbFace();
        String base64 = obj.getString("data");
        byte[] bytes = Base64.getDecoder().decode(base64);
        MatOfByte matOfByte = new MatOfByte(bytes);
        Mat rgbMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
        if (rgbMat.empty()) {
            throw new RuntimeException("图片为空");
        }
        long nativeObjAddr = rgbMat.getNativeObjAddr();
        String s = Face.identifyWithAllByMat(nativeObjAddr, 0);
        FaceRecognitionResponse faceRecognitionResponse = JSONObject.parseObject(s, FaceRecognitionResponse.class);

        System.out.println(s);
        return faceRecognitionResponse;
    }

    /**
     * 人脸可用性检测
     *
     * @param rgbMatAddr
     * @param userName
     * @return
     */
    public static Reply availableDetection(long rgbMatAddr, String userName, String type) {
        Reply reply = new Reply();
        reply.setType(type);
        try {
            Face.loadDbFace();
            String s = Face.identifyWithAllByMat(rgbMatAddr, 0);
            System.out.println("可用性检测结果:");
            System.out.println(s);
            FaceRecognitionResponse faceRecognitionResponse = JSONObject.parseObject(s, FaceRecognitionResponse.class);
            List<FaceRecognitionResult> faceRecognitionResults = faceRecognitionResponse.getData().getResult();
            if (faceRecognitionResults == null || faceRecognitionResults.size() < 1) {
                return null;
            }
            FaceRecognitionResult best = faceRecognitionResults.get(0);
            if (best.getScore() < 80) {
                return null;
            }
            UserService userService = new UserService();
            User userByUserName = userService.getUserByUserName(best.getUserId());
            // 如果数据没有出错，是不会返回null的
            if (userByUserName == null) {
                reply.setErrorMessage("人脸数据出现异常，请检查");
                return reply;
            }
            if ((userName == null || userName.isEmpty()) && userByUserName.getUserName() != null
                    || (userName != null && !userName.isEmpty() && !userName.equals(userByUserName.getUserName()))) {
                reply.setErrorMessage("人脸已和" + userByUserName.getName() + "绑定");
                return reply;
            }
            return null;
        } catch (Exception e) {
            reply.setErrorMessage(e.getMessage());
            e.printStackTrace();
            return reply;
        }
    }

    public static void init() {
        System.out.println("开始--重新生成百度人脸数据库");
        UserService userService = new UserService();
        Map<String, String> userIdWithFacePath = userService.getUserIdWithFacePath();
        userIdWithFacePath.forEach((userName, facePath) -> {
            System.out.println("用户名=" + userName + ", 人脸路径=" + facePath);
            Mat mat = Imgcodecs.imread(facePath);
            long matAddr = mat.getNativeObjAddr();
            String res = Face.userAddByMat(matAddr, userName, SystemConstant.BAIDU_FACE_DB_DEFAULT_GROUP, "无信息");
            mat.release();
        });
        System.out.println("结束--重新生成百度人脸数据库");
    }
}
