import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {

        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(20 * 1024 * 1024));
        ch.pipeline().addLast(new WebSocketServerProtocolHandler(
                "/ws", null, true, 15 * 1024 * 1024
        ));
        ch.pipeline().addLast(new WebSocketFrameAggregator(15 * 1024 * 1024));
        ch.pipeline().addLast(new TextFrameHandler());
        ch.pipeline().addLast(new BinaryFrameHandler());
    }
}
