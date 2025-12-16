public class FaceDemo {
    public static void main(String[] args) {
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
