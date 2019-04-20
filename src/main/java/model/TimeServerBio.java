package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * BIO Server
 *
 * @author nextGood
 * @date 2019/4/7
 */
public class TimeServerBio {

    public static void main(String[] args) {
        int port = 8080;
        try {
            if (null != args && args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("The time server is start in port:" + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new TimeServerHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TimeServerHandler implements Runnable {
        private Socket socket;

        public TimeServerHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String readLine = null;
                String currentTime = null;
                while (true) {
                    readLine = in.readLine();
                    if (null == readLine) break;
                    System.out.println("The time server receives order : " + readLine);
                    currentTime = "QUERY TIME ORDER".equalsIgnoreCase(readLine) ? new Date().toString() : "BAD ORDER";
                    out.println(currentTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    try {
                        in.close();
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