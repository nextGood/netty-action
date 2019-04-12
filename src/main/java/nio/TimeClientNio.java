package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO Client
 *
 * @author nextGood
 * @date 2019/4/9
 */
public class TimeClientNio {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(new TimeClientHandle("127.0.0.1", port)).start();
    }

    private static class TimeClientHandle implements Runnable {
        private String host;
        private int port;
        private Selector selector;
        private SocketChannel socketChannel;
        private boolean stop;

        public TimeClientHandle(String host, int port) {
            try {
                this.host = host == null ? "127.0.0.1" : host;
                this.port = port;
                selector = Selector.open();
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            try {
                doConnect();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
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
                            handleInput(key);
                        } catch (Exception e) {
                            if (null != key) {
                                key.cancel();
                                if (null != key.channel()) {
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

        private void handleInput(SelectionKey key) throws IOException {
            if (key.isValid()) {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                if (key.isConnectable() && socketChannel.finishConnect()) {
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    // 为什么刚注册了读操作就进行写数据了
                    doWrite(socketChannel);
                } else {
                    System.exit(1);
                }
                if (key.isReadable()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
                    int readBytes = socketChannel.read(byteBuffer);
                    if (readBytes > 0) {
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");
                        System.out.println("Now is :" + body);
                        this.stop = true;
                    } else if (readBytes < 0) {
                        key.cancel();
                        socketChannel.close();
                    } else {
                        System.out.println("读到0字节");
                    }
                }
            }
        }

        private void doConnect() throws IOException {
            if (socketChannel.connect(new InetSocketAddress(host, port))) {
                socketChannel.register(selector, SelectionKey.OP_READ);
                doWrite(socketChannel);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
        }

        private void doWrite(SocketChannel socketChannel) throws IOException {
            byte[] bytes = "QUERY TIME SERVER".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
            if (!writeBuffer.hasRemaining()) {
                System.out.println("Send order 2 server succeed.");
            }
        }
    }
}