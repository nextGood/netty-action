package model2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

/**
 * BIO Server
 *
 * @author nextGood
 * @date 2019/6/4
 */
public class TimeServerBio {
    private static volatile Integer number = 0;
    private static final int PORT = 8080;
    private static final String COMMAND = "query server time";

    public static void main(String[] args) throws Exception {
        System.out.println("启动端口:" + PORT);
        ServerSocket server = new ServerSocket(PORT);
        Socket socket = server.accept();
        System.out.println("端口:" + PORT + "连接数:" + number++);
        new Thread(new BusinessThread(socket)).start();
    }

    private static class BusinessThread implements Runnable {
        private Socket socket;

        public BusinessThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            PrintWriter writer = null;
            try {
                inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String request = reader.readLine();
                String response = "bad command";
                if (COMMAND.equalsIgnoreCase(request)) {
                    Calendar calendar = Calendar.getInstance();
                    Date nowTime = calendar.getTime();
                    response = nowTime.toString();
                }
                writer = new PrintWriter(socket.getOutputStream());
                writer.print(response);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != writer) {
                    writer.close();
                }
                if (null != inputStream) {
                    try {
                        inputStream.close();
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