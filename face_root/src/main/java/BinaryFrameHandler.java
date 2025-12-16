import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

public class BinaryFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    // ① 客户端发消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        try {
            ByteBuf buf = frame.content();
            int readableBytes = buf.readableBytes();

            // 调试打印：检查 1.0 质量下的图片到底有多大
            System.out.println("收到二进制帧，大小: " + (readableBytes / 1024) + " KB");

            byte[] data = new byte[readableBytes];
            buf.readBytes(data);

            // 使用 ByteArrayInputStream 包装
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                // 引入 TwelveMonkeys 后，ImageIO.read 会变得非常强大，能处理各种不标准 JPEG
                BufferedImage img = ImageIO.read(bais);

                if (img == null) {
                    System.err.println("错误：无法解析图片，ImageIO 返回 null。数据开头 10 字节: " +
                            bytesToHex(data, 10));
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("error_decode"));
                    return;
                }

                File dir = new File("C:\\Users\\Alice\\Desktop\\face_test2");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, System.currentTimeMillis() + ".jpg");

                // 注意：写入时也可以指定质量，但 ImageIO.write 默认对 JPEG 支持一般
                // 如果 ImageIO.write 仍然生成灰色，可以尝试存为 "png" 测试是否为编码问题
                boolean success = ImageIO.write(img, "jpg", file);

                if (success) {
                    System.out.println("保存成功: " + file.getAbsolutePath());
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("ack"));
                } else {
                    System.err.println("写入失败：找不到合适的写入器");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.channel().writeAndFlush(new TextWebSocketFrame("error_server"));
        }
    }

    private String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, len); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }

    // 客户端连接
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接：" + ctx.channel().id());
    }

    // 客户端断开
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端断开：" + ctx.channel().id());
    }

    // 异常处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}