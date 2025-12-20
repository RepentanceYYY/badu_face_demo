package constants;

public class SystemConstant {
    /**
     * websocket服务器占用端口
     */
    public static final int WEB_SOCKET_PORT = 8080;
    /**
     * 百度人脸模型路径
     */
    public static final String BAIDU_FACE_MODEL_PATH = "E:\\LZH\\Baidu_Face_Offline_SDK_Windows_Java_8.4\\face-native";
    /**
     * 百度人脸数据库默认组
     */
    public static final String BAIDU_FACE_DB_DEFAULT_GROUP = "group";
    /**
     * 数据库连接字符串
     */
    public static final String DB_URL = "jdbc:mysql://localhost:3306/rfidcabinet?useSSL=false&serverTimezone=UTC";
    /**
     * 人脸图片路径
     */
    public static final String FACE_IMAGE_PATH = "E:\\LZH\\rfid_cabinet_summary\\rfid_cabinet_client\\project-root\\static\\face\\faces";
    /**
     * 数据库用户名
     */
    public static final String DB_USERNAME = "root";
    /**
     * 数据库密码
     */
    public static final String DB_PASSWORD = "123456";
    public static final String WEB_SOCKET_PATH="/ws";
}
