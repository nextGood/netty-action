package model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * AIO Server
 *
 * @author nextGood
 * @date 2019/4/12
 */
public class TimeServerAio {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        AsyncTimeServerHandler timeServerHandler = new AsyncTimeServerHandler(port);
        new Thread(timeServerHandler, "AsyncTimeServer").start();
    }

    private static class AsyncTimeServerHandler implements Runnable {
        private int port;
        private CountDownLatch countDownLatch;
        private AsynchronousServerSocketChannel serverSocketChannel;

        public AsyncTimeServerHandler(int port) {
            this.port = port;
            try {
                serverSocketChannel = AsynchronousServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                System.out.println("The time server is start in port : " + port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            countDownLatch = new CountDownLatch(1);
            doAccept();
            try {
                countDownLatch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void doAccept() {
            serverSocketChannel.accept(this, new AcceptCompletionHandler());
        }

        private class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {
            @Override
            public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
                attachment.serverSocketChannel.accept(attachment, this);
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                result.read(byteBuffer, byteBuffer, new ReadCompletionHandler(result));
            }

            @Override
            public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
                exc.printStackTrace();
                attachment.countDownLatch.countDown();
            }
        }

        private class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {
            private AsynchronousSocketChannel channel;

            public ReadCompletionHandler(AsynchronousSocketChannel channel) {
                if (this.channel == null) {
                    this.channel = channel;
                }
            }

            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                attachment.flip();
                byte[] body = new byte[attachment.remaining()];
                attachment.get(body);
                try {
                    String req = new String(body, "UTF-8");
                    System.out.println("The time server receive order : " + req);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req) ? new Date().toString() : "BAD ORDER";
                    doWrite(currentTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void doWrite(String currentTime) {
                if (null != currentTime && currentTime.trim().length() > 0) {
                    byte[] bytes = currentTime.getBytes();
                    ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
                    writeBuffer.put(bytes);
                    writeBuffer.flip();
                    channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer buffer) {
                            if (buffer.hasRemaining()) {
                                channel.write(buffer, buffer, this);
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                try {
                    this.channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}