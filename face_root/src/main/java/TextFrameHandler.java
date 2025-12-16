import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String json = frame.text();

        JSONObject obj = JSONObject.parseObject(json);
        String type = obj.getString("type");

        if ("login".equals(type)) {
            String userId = obj.getString("userId");
            ConnectionManager.add(userId, ctx.channel());
            ctx.channel().writeAndFlush(new TextWebSocketFrame("login_ack"));
        } else if ("frame".equals(type)) {
            String base64 = obj.getString("data");
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) {
                    System.err.println("收到的 base64 不是有效图片");
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("error"));
                    return;
                }

                // 保存图片到本地
                File dir = new File("C:\\Users\\Alice\\Desktop\\face_test_base64");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, System.currentTimeMillis() + ".jpg");
                ImageIO.write(img, "jpg", file);

                // 回复 ACK
                ctx.channel().writeAndFlush(new TextWebSocketFrame("ack"));
                System.out.println("保存成功：" + file.getAbsolutePath());

            } catch (IllegalArgumentException | IOException e) {
                e.printStackTrace();
                ctx.channel().writeAndFlush(new TextWebSocketFrame("error"));
            }
        } else {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("unknown_type"));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接：" + ctx.channel().id());
        // 可以暂时不用存，等客户端发送登录信息
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端断开：" + ctx.channel().id());
        // 如果你存的是 userId → Channel，需要找到对应的 userId 并删除
        ConnectionManager.all().forEach(ch -> {
            if (ch == ctx.channel()) {
                ConnectionManager.remove(ch.id().asLongText());
            }
        });
    }

}
