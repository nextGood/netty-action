package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * 支持TCP粘包的Netty Client
 *
 * @author nextGood
 * @date 2019/4/22
 */
public class TimeClientPacakge {
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
        new TimeClientPacakge().connect("127.0.0.1", port);
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
                            // LineBasedFrameDecoder和StringDecoder组合成按行切换的文本解析器
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new TimeClientHandle());
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

    private class TimeClientHandle extends ChannelHandlerAdapter {
        private int counter;
        private byte[] req;

        public TimeClientHandle() {
            req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 当客户端和服务端TCP链路建立成功之后，Netty的NIO线程会调用channelActive方法，发送指令给服务端
            for (int i = 0; i < 100; i++) {
                ByteBuf message = Unpooled.buffer(req.length);
                message.writeBytes(req);
                ctx.writeAndFlush(message);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 当服务端返回应答消息时，channelRead会被调用
            String body = (String) msg;
            System.out.println("Now is:" + body + " ; the counter is : " + ++counter);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("Unexpected exception from downstream : " + cause.getMessage());
            ctx.close();
        }
    }
}