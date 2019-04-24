package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * Netty的protobuf客户端
 *
 * @author nextGood
 * @date 2019/4/24
 */
public class ProtoClient {
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
        new ProtoClient().connect("127.0.0.1", port);
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
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().addLast(new ProtobufDecoder(PersonProto.Person.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new ProtoClientHandle());
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

    private class ProtoClientHandle extends ChannelHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(wrapPerson());
        }

        private PersonProto.Person wrapPerson() {
            PersonProto.Person.Builder personBuilder = PersonProto.Person.newBuilder();
            personBuilder.setName("XXX");
            personBuilder.setEmail("xxx@qq.com");
            personBuilder.setId(111);

            PersonProto.Person.PhoneNumber.Builder phoneNumberBuilder = PersonProto.Person.PhoneNumber.newBuilder();
            phoneNumberBuilder.setNumber("000");
            phoneNumberBuilder.setType(PersonProto.Person.PhoneType.HOME);

            return personBuilder.build();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("Receive server response : [" + msg + "]");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}