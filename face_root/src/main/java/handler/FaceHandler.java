package handler;

import com.alibaba.fastjson2.JSONObject;
import com.jni.face.Face;
import com.jni.struct.EyeClose;
import com.jni.struct.HeadPose;
import com.jni.struct.LivenessInfo;
import config.SystemConfig;
import entity.Reply;
import entity.baidu.FaceRecognitionResponse;
import entity.baidu.FaceRecognitionResult;
import entity.db.User;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import server.UserService;
import utils.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class FaceHandler {
    public static int baiduFaceApiCode = 0;
    private static SystemConfig systemConfig = SystemConfig.getInstance();

    /**
     * 人脸采集
     *
     * @param obj
     * @return
     */
    public static Reply capture(JSONObject obj) {
        String base64Frame = obj.getString("frame");
        Object userNameObject = obj.get("userName");
        String userName = null;
        if (userNameObject != null) {
            userName = userNameObject.toString();
        }
        Object actionObject = obj.get("action");
        Mat rgbMat = null;
        Reply reply = new Reply();
        reply.setType("capture");
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

            // 人脸可用性检测(判断是否和其他人脸做绑定了什么的)
            Reply availableReply = availableDetection(rgbMatAddr, userName, "capture");
            if (availableReply != null) {
                return availableReply;
            }
            // 获取到嘴巴闭合参数
            float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
            // 如果有动作要求，先返回动作要求的结果
            if (actionObject != null) {
                return actionDetection(reply.getType(), mouthCloseScore, actionObject.toString(), rgbMatAddr);
            }
            // 嘴巴闭合检测
            if (mouthCloseScore[0] < 0.9f) {
                reply.setHintMessage("请闭合嘴巴");
                return reply;
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

            // 人脸模糊度检测
            float[] blurList = Face.faceBlur(rgbMatAddr);
            if (blurList == null || blurList.length <= 0) {
                reply.setHintMessage("检测不到人脸");
                return reply;
            }
            System.out.println("当前模糊度:" + blurList[0]);
            if (blurList[0] > 0.2f) {
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

    /**
     * 人脸认证
     *
     * @param obj
     * @return
     */
    public static Reply auth(JSONObject obj) {
        String base64Frame = obj.getString("frame");
        Object actionObject = obj.get("action");
        Mat rgbMat = null;
        Reply reply = new Reply();
        reply.setType("auth");
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
                reply.setHintMessage("未检测到人脸");
                return reply;
            }
            float liveScore = liveInfos[0].livescore;
            if (liveScore < 0.6f) {
                System.out.println(String.format("检测到非活体,%.3f", liveScore));
                reply.setHintMessage("未检测到人脸");
                return reply;
            }
            // 如果当前是动作检测
            if (actionObject != null) {
                // 获取到嘴巴闭合参数
                float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
                return actionDetection(reply.getType(), mouthCloseScore, actionObject.toString(), rgbMatAddr);
            }

            Face.loadDbFace();
            String s = Face.identifyWithAllByMat(rgbMatAddr, 0);
            System.out.println(s);
            FaceRecognitionResponse faceRecognitionResponse = JSONObject.parseObject(s, FaceRecognitionResponse.class);
            List<FaceRecognitionResult> faceRecognitionResults = faceRecognitionResponse.getData().getResult();
            if (faceRecognitionResults == null || faceRecognitionResults.size() < 1) {
                reply.setErrorMessage("人脸不存在");
                return reply;
            }
            FaceRecognitionResult best = faceRecognitionResults.get(0);
            if (best.getScore() < 80) {
                reply.setErrorMessage("人脸不存在");
                return reply;
            }
            UserService userService = new UserService();
            User userByUserName = userService.getUserByUserName(best.getUserId());
            if (userByUserName == null) {
                reply.setErrorMessage("人脸数据出现异常，请检查");
                return reply;
            }
            reply.setSuccessMessage("登录成功");
            reply.setData(userByUserName);
            return reply;

        } catch (Exception e) {
            reply.setErrorMessage(e.getMessage());
            return reply;
        } finally {
            rgbMat.release();
        }
    }

    /**
     * 人脸注册和更新
     *
     * @param obj
     * @return
     */
    public static Reply update(JSONObject obj) {
        Reply reply = new Reply();
        String base64Frame = obj.getString("frame");
        Object userNameObject = obj.get("userName");
        String userName;
        if (userNameObject == null) {
            reply.setErrorMessage("未提供用户账号");
            return reply;
        }
        userName = userNameObject.toString();
        Mat rgbMat = null;
        reply.setType("update");
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Frame);
            MatOfByte matOfByte = new MatOfByte(bytes);
            rgbMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
            if (rgbMat.empty()) {
                reply.setErrorMessage("未提供图片base64");
                return reply;
            }
            long rgbMatAddr = rgbMat.getNativeObjAddr();
            // 静默活体检测
            LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
            if (liveInfos == null || liveInfos.length <= 0 || liveInfos[0].box == null) {
                reply.setHintMessage("未检测到人脸");
                return reply;
            }
            String addResult = Face.userAddByMat(rgbMatAddr, userName, systemConfig.getBaiduFaceDbDefaultGroup(), "notInfo");
            String updateResult = Face.userUpdate(rgbMatAddr, userName, systemConfig.getBaiduFaceDbDefaultGroup(), "notInfo");
            reply.setSuccessMessage("更新成功");
        } catch (Exception e) {
            reply.setErrorMessage("更新失败");
            return reply;
        } finally {
            rgbMat.release();
        }

        return reply;
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

    /**
     * 动作检测
     *
     * @param type
     * @param mouthCloseScore
     * @param action
     * @param rgbMatAddr
     * @return
     */
    public static Reply actionDetection(String type, float[] mouthCloseScore, String action, long rgbMatAddr) {
        Reply reply = new Reply();
        reply.setType(type);
        HeadPose[] headPoses = Face.faceHeadPose(rgbMatAddr);
        if (headPoses == null || headPoses.length <= 0) {
            reply.setHintMessage("检测不到人脸");
            return reply;
        }
        if (action.equals("turn_left")) {
            if (headPoses[0].yaw > 20F) {
                reply.setHintMessage("动作完成");
                reply.setActionCompleted(true);
                return reply;
            } else {
                reply.setHintMessage("请左转头");
                reply.setActionCompleted(false);
                return reply;
            }
        }
        if (action.equals("turn_right")) {
            if (headPoses[0].yaw < -20F) {
                reply.setHintMessage("动作完成");
                reply.setActionCompleted(true);
                return reply;
            } else {
                reply.setHintMessage("请右转头");
                reply.setActionCompleted(false);
                return reply;
            }
        }
        if (action.equals("open_mouth")) {
            if (mouthCloseScore[0] < 0.6f) {
                reply.setHintMessage("动作完成");
                reply.setActionCompleted(true);
                return reply;
            } else {
                reply.setHintMessage("请张嘴");
                reply.setActionCompleted(false);
                return reply;
            }
        }
        reply.setErrorMessage("动作检测异常");
        return reply;
    }

    /**
     * 初始化百度人脸数据库
     */
    public static void init() {
        System.out.println("开始--> 重新生成百度人脸数据库");
        long startTime = System.currentTimeMillis();
        UserService userService = new UserService();
        Map<String, String> userIdWithFacePath = userService.getUserIdWithFacePath();
        userIdWithFacePath.forEach((userName, facePath) -> {
            System.out.println("用户名=" + userName + ", 人脸路径=" + facePath);
            Mat mat = Imgcodecs.imread(facePath);
            long matAddr = mat.getNativeObjAddr();
            String res = Face.userAddByMat(matAddr, userName, systemConfig.getBaiduFaceDbDefaultGroup(), "无信息");
            mat.release();
        });
        long endTime = System.currentTimeMillis(); // 结束计时
        long duration = endTime - startTime;
        System.out.println("结束--> 重新生成百度人脸数据库，耗时：" + duration + " 毫秒");
    }


}
