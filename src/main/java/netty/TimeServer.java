package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Date;

/**
 * Netty Server
 *
 * @author nextGood
 * @date 2019/4/15
 */
public class TimeServer {
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
        new TimeServer().bind(port);
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
                    .childHandler(new ChildChannelHandle());
            // 绑定监听端口，同步阻塞等待绑定操作完成
            ChannelFuture future = bootstrap.bind(port).sync();
            // 同步阻塞等待服务端链路关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放 NIO 线程组资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandle extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new TimeServerHandle());
        }
    }

    private class TimeServerHandle extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            // 获取缓冲区中可读字节数
            byte[] bytes = new byte[byteBuf.readableBytes()];
            // 将缓冲区中的字节数组复制到新建的byte数组中
            byteBuf.readBytes(bytes);
            String body = new String(bytes, "UTF-8");
            System.out.println("The time server receive order : " + body);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
            ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
            // 将待发送的消息放到消息发送队列中
            ctx.write(resp);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // 将消息发送队列中的消息写入到SocketChannel中发送给对方
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}