package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * FixedLengthFrameDecoder的Netty Server
 *
 * @author nextGood
 * @date 2019/4/23
 */
public class FixedLengthFrameDecoderServer {
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
        new FixedLengthFrameDecoderServer().bind(port);
    }

    private void bind(int port) {
        // 用于服务端接受客户端连接的Reactor线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用于进行SocketChannel网络读写的Reactor线程组
        EventLoopGroup workGroup = new NioEventLoopGroup();
        // Netty用于启动NIO服务端的辅助启动类
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 对采用分隔符做码流结束标识的消息进行解码
                            socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(20));
                            // 将ByteBuf解码成字符串对象
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(new EchoServerHandle());
                        }
                    });
            // 同步阻塞等待绑定操作完成
            ChannelFuture future = bootstrap.bind(port).sync();
            // 同步阻塞等待服务端链路关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private class EchoServerHandle extends ChannelHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String body = (String) msg;
            System.out.println("Client receive [" + body + "]");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}