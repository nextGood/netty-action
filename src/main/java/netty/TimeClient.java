package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty Client
 *
 * @author nextGood
 * @date 2019/4/15
 */
public class TimeClient {
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
        new TimeClient().connect("127.0.0.1", port);
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
                            socketChannel.pipeline().addLast(new TimeServerHandle());
                        }
                    });
            // 发起异步连接，同步等待连接成功
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 发起异步关闭客户端链路，同步等待链路关闭成功
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放 NIO 线程组资源
            group.shutdownGracefully();
        }
    }

    private class TimeServerHandle extends ChannelHandlerAdapter {
        private final ByteBuf byteBuf;

        public TimeServerHandle() {
            byte[] req = "QUERY TIME ORDER".getBytes();
            byteBuf = Unpooled.buffer(req.length);
            byteBuf.writeBytes(req);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 当客户端和服务端TCP链路建立成功之后，Netty的NIO线程会调用channelActive方法，发送指令给服务端
            ctx.writeAndFlush(byteBuf);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 当服务端返回应答消息时，channelRead会被调用
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] resp = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(resp);
            String body = new String(resp, "UTF-8");
            System.out.println("Now is:" + body);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("Unexpected exception from downstream : " + cause.getMessage());
            ctx.close();
        }
    }
}