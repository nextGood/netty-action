package model2;

import java.io.OutputStream;
import java.net.Socket;

/**
 * BIO Client
 *
 * @author nextGood
 * @date 2019/6/4
 */
public class TimeClientBio {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String host = "127.0.0.1";
        Socket socket = new Socket(host,port);
        OutputStream outputStream = socket.getOutputStream();
    }
}