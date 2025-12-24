package handler;

import com.jni.face.Face;
import entity.Reply;
import entity.baidu.FaceRecognitionResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import manager.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String originalText = frame.text();
        JSONObject obj = JSONObject.parseObject(originalText);
        String type = obj.getString("type");
        if (FaceHandler.baiduFaceApiCode != 0) {
            Reply commReply = new Reply();
            commReply.setType(type);
            commReply.setErrorMessage("人脸模型初始化失败，状态码为" + FaceHandler.baiduFaceApiCode);
            ctx.channel().writeAndFlush(
                    new TextWebSocketFrame(JSON.toJSONString(commReply))
            );
            return;
        }
        switch (type) {

            case "auth": {
                Reply auth = FaceHandler.auth(obj);
                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame(JSON.toJSONString(auth))
                );
                break;
            }

            case "capture": {
                Reply reply = FaceHandler.capture(obj);
                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame(JSON.toJSONString(reply))
                );
                break;
            }

            case "update": {
                Reply reply = FaceHandler.update(obj);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(reply)));
                break;
            }

            case "faceRecognition": {
                try {
                    FaceRecognitionResponse faceRecognitionResponse =
                            FaceHandler.faceRecognition(obj);

                    Map<String, Object> res = new HashMap<>();
                    res.put("reply", "ack");
                    res.put("type", "faceRecognition");
                    res.put("data", faceRecognitionResponse);

                    ctx.channel().writeAndFlush(
                            new TextWebSocketFrame(JSON.toJSONString(res))
                    );
                } catch (Exception e) {
                    e.printStackTrace();

                    Map<String, Object> res = new HashMap<>();
                    res.put("reply", "error");

                    ctx.channel().writeAndFlush(
                            new TextWebSocketFrame(JSON.toJSONString(res))
                    );
                }
                break;
            }

            default: {
                Map<String, Object> res = new HashMap<>();
                res.put("reply", "unknown_type");

                ctx.channel().writeAndFlush(
                        new TextWebSocketFrame(JSON.toJSONString(res))
                );
                break;
            }
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
