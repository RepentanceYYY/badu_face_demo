import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.HashMap;
import java.util.Map;

public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String originalText = frame.text();
        JSONObject obj = JSONObject.parseObject(originalText);
        String type = obj.getString("type");

        if ("login".equals(type)) {
            String userId = obj.getString("userId");
            ConnectionManager.add(userId, ctx.channel());
            ctx.channel().writeAndFlush(new TextWebSocketFrame("login_ack"));
        } else if ("frame".equals(type)) {
            try {
                FaceLivenessResult faceLivenessResult = FaceHandler.bestFace(obj);
                Map<String, Object> res = new HashMap<>();
                res.put("reply", "ack");
                res.put("faceLivenessResult", faceLivenessResult);
                String resultJson = JSON.toJSONString(res);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(resultJson));
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, Object> res = new HashMap<>();
                res.put("reply", "error");
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(res)));
            }
        } else {
            Map<String, Object> res = new HashMap<>();
            res.put("reply", "unknown_type");
            ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(res)));
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
