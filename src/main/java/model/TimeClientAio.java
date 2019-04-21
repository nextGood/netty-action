package model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * AIO Client
 *
 * @author nextGood
 * @date 2019/4/12
 */
public class TimeClientAio {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(new AsyncTimeClientHandler("127.0.0.1", port), "AIO-AsyncTimeClientHandler-001").start();
    }

    private static class AsyncTimeClientHandler implements CompletionHandler<Void, AsyncTimeClientHandler>, Runnable {
        private String host;
        private int port;
        private CountDownLatch downLatch;
        private AsynchronousSocketChannel socketChannel;

        public AsyncTimeClientHandler(String host, int port) {
            this.host = host;
            this.port = port;
            try {
                socketChannel = AsynchronousSocketChannel.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            downLatch = new CountDownLatch(1);
            socketChannel.connect(new InetSocketAddress(host, port), this, this);
            try {
                downLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void completed(Void result, AsyncTimeClientHandler attachment) {
            byte[] bytes = "QUERY TIME ORDER".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            socketChannel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (attachment.hasRemaining()) {
                        socketChannel.write(attachment, attachment, this);
                    } else {
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        socketChannel.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer attachment) {
                                attachment.flip();
                                byte[] bytes = new byte[attachment.remaining()];
                                attachment.get(bytes);
                                String body;
                                try {
                                    body = new String(bytes, "UTF-8");
                                    System.out.println("Now is : " + body);
                                    downLatch.countDown();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try {
                                    socketChannel.close();
                                    downLatch.countDown();
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
                        socketChannel.close();
                        downLatch.countDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
            exc.printStackTrace();
            try {
                socketChannel.close();
                downLatch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}