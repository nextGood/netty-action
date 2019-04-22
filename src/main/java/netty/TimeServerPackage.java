package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.Date;

/**
 * 支持TCP粘包的Netty Server
 *
 * @author nextGood
 * @date 2019/4/22
 */
public class TimeServerPackage {
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
        new TimeServerPackage().bind(port);
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

    private class ChildChannelHandle extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            // LineBasedFrameDecoder和StringDecoder组合成按行切换的文本解析器
            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
            socketChannel.pipeline().addLast(new StringDecoder());
            socketChannel.pipeline().addLast(new TimeServerHandle());
        }
    }

    private class TimeServerHandle extends ChannelHandlerAdapter {
        private int counter;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String body = (String) msg;
            System.out.println("The time server receive order : " + body + " ; the counter is : " + ++counter);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
            currentTime = currentTime + System.getProperty("line.separator");
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