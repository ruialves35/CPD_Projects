package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5500);
            Socket socket = serverSocket.accept();
            DataInputStream stream = new DataInputStream(socket.getInputStream());

            String message = stream.readUTF();
            System.out.println(message);
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
