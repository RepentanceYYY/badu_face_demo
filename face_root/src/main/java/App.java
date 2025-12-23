import com.jni.face.Face;
import constants.SystemConstant;
import handler.FaceHandler;
import server.WebSocketServer;
import utils.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.util.Date;
import java.util.Iterator;

public class App {


    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        FileUtils.deleteOldBaiduFaceDb();
        // 强制扫描并注册 TwelveMonkeys 插件
        ImageIO.scanForPlugins();
        // 调试：打印当前可用的 JPEG 写入器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        while (writers.hasNext()) {
            ImageWriter w = writers.next();
            System.out.println("JPEG Writer: " + w.getClass().getName());
            // 如果看到 com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter 就说明成功
        }
        /*  sdk初始化 */
        Face api = new Face();
        int res = api.sdkInit(SystemConstant.BAIDU_FACE_MODEL_PATH);
        FaceHandler.baiduFaceApiCode = res;
        if (res == 0) {
            FaceHandler.init();
            Face.loadDbFace();
        }
        WebSocketServer server = new WebSocketServer(SystemConstant.WEB_SOCKET_PORT);
        long endTime = System.currentTimeMillis();
        // JVM 关闭时优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (FaceHandler.baiduFaceApiCode == 0) api.sdkDestroy();
            System.out.println("收到关闭信号，正在关闭 WebSocket server...");
            server.shutdown();
        }));
        try {
            server.start();
            long duration = endTime - startTime;
            long hours = duration / (1000 * 60 * 60);
            long minutes = (duration / (1000 * 60)) % 60;
            long seconds = (duration / 1000) % 60;
            long millis = duration % 1000;

            System.out.println(String.format("服务启动成功，总共花费时间: %02d时 %02d分 %02d秒 %03d毫秒",
                    hours, minutes, seconds, millis));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
