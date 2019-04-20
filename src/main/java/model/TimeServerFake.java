package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.*;

/**
 * 伪异步I/O Server
 *
 * @author nextGood
 * @date 2019/4/8
 */
public class TimeServerFake {
    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ServerSocket server = null;
        Socket socket = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The time server is start in port:" + port);
            TimeExecutorPool executorPool = new TimeExecutorPool(3, 15, 30);
            while (true) {
                socket = server.accept();
                executorPool.executor(new TimeServerTask(socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != server) {
                System.out.println("The time server close");
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class TimeExecutorPool {

        private ExecutorService executor;

        public TimeExecutorPool(int coreThread, int maxThread, int queueLength) {
            executor = new ThreadPoolExecutor(coreThread, maxThread, 120, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueLength), new RejectedExecutionHandler() {
                public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
                    System.out.println("Over the length of queue");
                }
            });
        }

        public void executor(Runnable runnable) {
            executor.execute(runnable);
        }
    }

    private static class TimeServerTask implements Runnable {
        private Socket socket;

        public TimeServerTask(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            BufferedReader bufferedReader = null;
            PrintWriter printWriter = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream(), true);
                String readLine = null;
                String currentTime = null;
                while (true) {
                    readLine = bufferedReader.readLine();
                    if (null == readLine) break;
                    System.out.println("The time server receive order : " + readLine);
                    currentTime = "QUERY TIME ORDER".equalsIgnoreCase(readLine) ? new Date().toString() : "BAD ORDER";
                    printWriter.println(currentTime);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != printWriter) {
                    printWriter.close();
                }
                if (null != bufferedReader) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (null != socket) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}