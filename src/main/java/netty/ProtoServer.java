package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * Netty的protobuf服务端
 *
 * @author nextGood
 * @date 2019/4/24
 */
public class ProtoServer {
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
        new ProtoServer().bind(port);
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
                            // ProtobufVarint32FrameDecoder用于处理半包
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().addLast(new ProtobufDecoder(PersonProto.Person.getDefaultInstance()));
                            //
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new ProtoServerHandle());
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

    private class ProtoServerHandle extends ChannelHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            PersonProto.Person person = (PersonProto.Person) msg;
            if ("XXX".equals(person.getName())) {
                System.out.println("Service accept client message : [" + person.getName() + "]");
                ctx.writeAndFlush(resp(person));
            }
        }

        private PersonProto.Person resp(PersonProto.Person person) {
            PersonProto.Person.Builder personBuilder = PersonProto.Person.newBuilder();
            personBuilder.setName(person.getName());
            personBuilder.setEmail(person.getEmail());
            personBuilder.setId(person.getId());

            PersonProto.Person.PhoneNumber.Builder phoneNumberBuilder = PersonProto.Person.PhoneNumber.newBuilder();
            phoneNumberBuilder.setNumber("000");
            phoneNumberBuilder.setType(PersonProto.Person.PhoneType.HOME);

            return personBuilder.build();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}