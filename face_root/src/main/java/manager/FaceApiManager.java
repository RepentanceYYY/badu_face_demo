package manager;

import com.alibaba.fastjson2.JSONObject;
import com.jni.face.Face;
import config.SystemConfig;
import entity.Reply;
import handler.FaceHandler;
import utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FaceApiManager {
    public static Face api = new Face();
    /**
     * sdk初始化响应码
     */
    public static int sdkInitCode = 0;
    public static Map<Integer, String> codeTextMap = new HashMap<>();


    /**
     * 初始化Api
     */
    public static void load() {
        System.out.println("开始--> 删除百度人脸旧数据库");
        Path dir = Paths.get(System.getProperty("java.home"), "bin", "db");
        System.out.println("百度人脸数据库所在目录" + dir);
        FileUtils.deleteDirectory(dir);
        System.out.println("结束--> 删除百度人脸旧数据库");
        SystemConfig systemConfig = SystemConfig.getInstance();
        api = new Face();
        sdkInitCode = api.sdkInit(systemConfig.getBaiduFaceModelPath());
        System.out.println("百度人脸SDK初始化结果---> " + getErrorText(sdkInitCode));
        if (sdkInitCode == 0) {
            FaceHandler.init();
            Face.loadDbFace();
        } else {
            destroy(); // 销毁SDK，防止内存泄露
        }
    }

    /**
     * 卸载释放
     */
    public static void destroy() {
        api.sdkDestroy();
        System.out.println("SDK已卸载");
    }

    /**
     * 初始化百度人脸数据库
     */
    public static void initDatabase() {

    }

    /**
     * 激活SDK
     */
    public static Reply activateSDK(JSONObject obj) {
        Reply reply = new Reply();
        SystemConfig systemConfig = SystemConfig.getInstance();
        File licenseFile = new File(systemConfig.getBaiduFaceModelPath(), "license/license.key");
        if (!licenseFile.exists()) {
            reply.setErrorMessage("找不到证书路径，无法激活");
            return reply;
        }
        Object activationCodeObject = obj.get("activationCode");
        if (activationCodeObject == null) {
            reply.setErrorMessage("激活码不能为空");
            return reply;
        }
        String activationCode = activationCodeObject.toString().trim();
        FileWriter writer = null;
        try {
            writer = new FileWriter(licenseFile, false);
            writer.write(activationCode);
            writer.flush();
        } catch (IOException e) {
            reply.setErrorMessage("激活码写入失败,程序消息:"+e.getMessage());
            return reply;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        api = new Face();
        sdkInitCode = api.sdkInit(systemConfig.getBaiduFaceModelPath());
        if(sdkInitCode != 0){
            destroy();
            reply.setErrorMessage("激活失败，SDK回复:"+getErrorText(sdkInitCode));
            return reply;
        }
        FaceHandler.init();
        Face.loadDbFace();
        reply.setSuccessMessage("百度人脸激活成功");
        return reply;
    }

    /**
     * 根据错误码获取对应描述
     */
    public static String getErrorText(int code) {
        return codeTextMap.getOrDefault(code, "未知错误");
    }

    static {
        codeTextMap.put(0, "SUCCESS /成功");
        codeTextMap.put(-1, "ILLEGAL_PARAMS / 失败或非法参数");
        codeTextMap.put(-2, "MEMORY_ALLOCATION_FAILED / 内存分配失败");
        codeTextMap.put(-3, "INSTANCE_IS_EMPTY / 实例对象为空");
        codeTextMap.put(-4, "MODEL_IS_EMPTY / 模型内容为空");
        codeTextMap.put(-5, "UNSUPPORT_ABILITY_TYPE / 不支持的能力类型");
        codeTextMap.put(-6, "UNSUPPORT_INFER_TYPE / 不支持的预测库类型");
        codeTextMap.put(-7, "NN_CREATE_FAILED / 预测库对象创建失败");
        codeTextMap.put(-8, "NN_INIT_FAILED / 预测库对象初始化失败");
        codeTextMap.put(-9, "IMAGE_IS_EMPTY / 图像数据为空");
        codeTextMap.put(-10, "ABILITY_INIT_FAILED / 人脸能力初始化失败");
        codeTextMap.put(-11, "ABILITY_UNLOAD / 人脸能力未加载");
        codeTextMap.put(-12, "ABILITY_ALREADY_LOADED / 人脸能力已加载");
        codeTextMap.put(-13, "NOT_AUTHORIZED / 未授权");
        codeTextMap.put(-14, "ABILITY_RUN_EXCEPTION / 人脸能力运行异常");
        codeTextMap.put(-15, "UNSUPPORT_IMAGE_TYPE / 不支持的图像类型");
        codeTextMap.put(-16, "IMAGE_TRANSFORM_FAILED / 图像转换失败");
        codeTextMap.put(-1001, "SYSTEM_ERROR / 系统错误");
        codeTextMap.put(-1002, "PARARM_ERROR / 参数错误");
        codeTextMap.put(-1003, "DB_OP_FAILED / 数据库操作失败");
        codeTextMap.put(-1004, "NO_DATA / 没有数据");
        codeTextMap.put(-1005, "RECORD_UNEXIST / 记录不存在");
        codeTextMap.put(-1006, "RECORD_ALREADY_EXIST / 记录已经存在");
        codeTextMap.put(-1007, "FILE_NOT_EXIST / 文件不存在");
        codeTextMap.put(-1008, "GET_FEATURE_FAIL / 提取特征值失败");
        codeTextMap.put(-1009, "FILE_TOO_BIG / 文件太大");
        codeTextMap.put(-1010, "FACE_RESOURCE_NOT_EXIST / 人脸资源文件不存在");
        codeTextMap.put(-1011, "FEATURE_LEN_ERROR / 特征值长度错误");
        codeTextMap.put(-1012, "DETECT_NO_FACE / 未检测到人脸");
        codeTextMap.put(-1013, "CAMERA_ERROR / 摄像头错误或不存在");
        codeTextMap.put(-1014, "FACE_INSTANCE_ERROR / 人脸引擎初始化错误");
        codeTextMap.put(-1015, "LICENSE_FILE_NOT_EXIST / 授权文件不存在");
        codeTextMap.put(-1016, "LICENSE_KEY_EMPTY / 授权序列号为空");
        codeTextMap.put(-1017, "LICENSE_KEY_INVALID / 授权序列号无效");
        codeTextMap.put(-1018, "LICENSE_KEY_EXPIRE / 授权序列号过期");
        codeTextMap.put(-1019, "LICENSE_ALREADY_USED / 授权序列号已被使用");
        codeTextMap.put(-1020, "DEVICE_ID_EMPTY / 设备指纹为空");
        codeTextMap.put(-1021, "NETWORK_TIMEOUT / 网络超时");
        codeTextMap.put(-1022, "NETWORK_ERROR / 网络错误");
        codeTextMap.put(-1023, "CONF_INI_UNEXIST / 配置ini文件不存在");
        codeTextMap.put(-1024, "WINDOWS_SERVER_ERROR / 禁用在Windows Server");
    }
}
