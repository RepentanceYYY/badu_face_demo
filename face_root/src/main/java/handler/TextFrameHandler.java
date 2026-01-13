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
import manager.FaceApiManager;

import java.util.HashMap;
import java.util.Map;

public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String originalText = frame.text();
        JSONObject obj = JSONObject.parseObject(originalText);
        String type = obj.getString("type");
        /**
         * 激活百度人脸sdk
         */
        if (type.equals("activation")) {
            Reply reply = FaceApiManager.activateSDK(obj);
            reply.setType("activation");
            ctx.channel().writeAndFlush(
                    new TextWebSocketFrame(JSON.toJSONString(reply))
            );
            return;
        }
        /**
         * 获取激活状态
         */
        if (type.equals("activationStatus")) {
            Reply reply = new Reply();
            reply.setType("activationStatus");
            Map<Object, Object> dataMap = new HashMap<>();
            if (FaceApiManager.sdkInitCode <= 1015 && FaceApiManager.sdkInitCode >= -1019) {
                dataMap.put("needActivation", true);
                dataMap.put("activationCode", FaceApiManager.queryActivationCode());
            }
            dataMap.put("sdkInitCode", FaceApiManager.sdkInitCode);
            reply.setData(dataMap);
            if (FaceApiManager.sdkInitCode != 0) {
                reply.setErrorMessage(FaceApiManager.getErrorText(FaceApiManager.sdkInitCode));
            } else {
                reply.setSuccessMessage("百度人脸SDK已激活");
            }
            ctx.channel().writeAndFlush(
                    new TextWebSocketFrame(JSON.toJSONString(reply))
            );
            return;
        }
        /**
         * 下面的所有动作都需要百度人脸已经激活
         */
        if (FaceApiManager.sdkInitCode != 0) {
            Reply commReply = new Reply();
            commReply.setType(type);
            commReply.setErrorMessage(FaceApiManager.getErrorText(FaceApiManager.sdkInitCode));
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
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端断开：" + ctx.channel().id());
        ConnectionManager.all().forEach(ch -> {
            if (ch == ctx.channel()) {
                ConnectionManager.remove(ch.id().asLongText());
            }
        });
    }

}
