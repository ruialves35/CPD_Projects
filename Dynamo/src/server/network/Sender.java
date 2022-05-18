package server.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender {
    public static void sendMulticast(byte[] msg, String multicastIpAddr, int multicastIPPort) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(multicastIpAddr);

            DatagramPacket packet = new DatagramPacket(
                    msg,
                    msg.length,
                    group,
                    multicastIPPort);

            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
