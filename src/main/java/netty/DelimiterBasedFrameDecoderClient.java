package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * DelimiterBasedFrameDecoder的Netty Client
 *
 * @author nextGood
 * @date 2019/4/22
 */
public class DelimiterBasedFrameDecoderClient {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        new DelimiterBasedFrameDecoderClient().connect("127.0.0.1", port);
    }

    private void connect(String host, int port) {
        // 创建客户端处理I/O读写的Group线程组
        EventLoopGroup group = new NioEventLoopGroup();
        // 创建客户端辅助启动类
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel socketChannel) throws Exception {
                            ByteBuf byteBuf = Unpooled.copiedBuffer("$_".getBytes());
                            socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, byteBuf));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new EchoClientHandle());
                        }
                    });
            // 发起异步连接，同步等待连接成功
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 发起异步关闭客户端链路，同步等待链路关闭成功
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private class EchoClientHandle extends ChannelHandlerAdapter {
        private int counter;
        static final String ECHO_REQ = "Hi,Welcome to Netty.$_";

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            for (int i = 0; i < 100; i++) {
                ctx.writeAndFlush(Unpooled.copiedBuffer(ECHO_REQ.getBytes()));
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("This is " + ++counter + " times receive server : [" + msg + "]");
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}