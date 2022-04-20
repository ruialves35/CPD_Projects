package client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        Client.udpTest();
    }

    private static void tcpTest() {
        try {
            Socket socket = new Socket("localhost", 5500);
            DataOutputStream stream = new DataOutputStream(socket.getOutputStream());

            stream.writeUTF("Boas mano ta tudo");

            stream.flush();
            stream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void udpTest() {
        try {
            DatagramSocket socket = new DatagramSocket(5502);
            String message = "ola udp";
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getLocalHost(), 5501);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
