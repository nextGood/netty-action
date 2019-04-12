package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO Server
 *
 * @author nextGood
 * @date 2019/4/9
 */
public class TimeServerNio {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        new Thread(timeServer, "NIO-MultiplexerTimeServer-001").start();
    }

    private static class MultiplexerTimeServer implements Runnable {
        /**
         * 设置为全局变量好吗？
         */
        private Selector selector;
        private ServerSocketChannel serverSocketChannel;
        private volatile boolean stop;

        MultiplexerTimeServer(int port) {
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                // 为什么没有设置IP地址？
                serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("The time server is start in port:" + port);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void stop() {
            stop = true;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    selector.select(1000);
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey key = null;
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        iterator.remove();
                        try {
                            handleKey(key);
                        } catch (Exception e) {
                            if (null != key) {
                                key.cancel();
                                if (key.channel() != null) {
                                    key.channel().close();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != selector) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleKey(SelectionKey key) throws IOException {
            if (key.isValid()) {
                if (key.isAcceptable()) {
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = ssc.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
                if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int readBytes = socketChannel.read(byteBuffer);
                    if (readBytes > 0) {
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");
                        System.out.println("The time server receive order : " + body);
                        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                        doWrite(socketChannel, currentTime);
                    } else if (readBytes < 0) {
                        key.cancel();
                        socketChannel.close();
                    } else {
                        System.out.println("读到0字节");
                    }
                }
            }
        }

        private void doWrite(SocketChannel socketChannel, String response) throws IOException {
            if (null != response && response.trim().length() > 0) {
                byte[] bytes = response.getBytes();
                ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
                writeBuffer.put(bytes);
                writeBuffer.flip();
                socketChannel.write(writeBuffer);
            }
        }
    }
}