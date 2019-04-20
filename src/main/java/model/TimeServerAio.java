package model;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
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
            //serverSocketChannel.accept(this, new AcceptCompletionHandler());
        }
    }
}