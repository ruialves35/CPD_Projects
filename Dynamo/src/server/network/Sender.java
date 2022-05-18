package server.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

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

    public static void sendTCPMessage(byte[] msg, String ipAddr, int ipPort) {
        try {
            Socket socket = new Socket(ipAddr, ipPort);
            DataOutputStream stream = new DataOutputStream(socket.getOutputStream());

            stream.write(msg);

            stream.flush();
            stream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
