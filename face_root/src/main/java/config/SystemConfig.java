package config;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置单例
 */
@Data
public class SystemConfig {

    private static final SystemConfig INSTANCE;

    static {
        INSTANCE = loadConfig();
    }

    // 配置字段
    private int webSocketPort;
    private String baiduFaceModelPath;
    private String baiduFaceDbDefaultGroup;
    private String dbUrl;
    private String faceImagePath;
    private String dbUsername;
    private String dbPassword;
    private String webSocketPath;

    // 私有构造
    private SystemConfig() {
    }

    /**
     * 获取单例实例
     */
    public static SystemConfig getInstance() {
        return INSTANCE;
    }

    /**
     * 加载配置
     */
    private static SystemConfig loadConfig() {
        try (InputStream is = SystemConfig.class.getClassLoader()
                .getResourceAsStream("config/system.json")) {
            if (is == null) {
                throw new RuntimeException("无法找到 system.json");
            }
            String jsonStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(jsonStr, SystemConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("加载 system.json 失败", e);
        }
    }
}
