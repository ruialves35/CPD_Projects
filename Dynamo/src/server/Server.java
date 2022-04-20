package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        Server.udpTest();
    }

    private static void tcpTest() {
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

    private static void udpTest() {
        try {
            byte[] buffer = new byte[100];
            DatagramSocket socket = new DatagramSocket(5501);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String message = new String(buffer);
            System.out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
