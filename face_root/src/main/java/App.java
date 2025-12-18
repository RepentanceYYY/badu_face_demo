import com.jni.face.Face;
import server.WebSocketServer;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.util.Iterator;

public class App {
    public static void main(String[] args) {
        /*  sdk初始化 */
        Face api = new Face();
        String modelPath = "E:\\LZH\\Baidu_Face_Offline_SDK_Windows_Java_8.4\\face-native";
        int res = api.sdkInit(modelPath);
        if (res != 0) {
            System.out.printf("sdk init fail and error =%d\n", res);
            return;
        }
        // 强制扫描并注册 TwelveMonkeys 插件
        ImageIO.scanForPlugins();

        // 调试：打印当前可用的 JPEG 写入器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        while (writers.hasNext()) {
            ImageWriter w = writers.next();
            System.out.println("JPEG Writer: " + w.getClass().getName());
            // 如果看到 com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter 就说明成功
        }
        int port = 8080;
        WebSocketServer server = new WebSocketServer(port);
        // JVM 关闭时优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("收到关闭信号，正在关闭 WebSocket server...");
            server.shutdown();
        }));
        try {
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
