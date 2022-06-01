package common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Sender {
    public static void sendMulticast(byte[] msg, String multicastIpAddr, int multicastIPPort) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(multicastIpAddr);

        DatagramPacket packet = new DatagramPacket(
                msg,
                msg.length,
                group,
                multicastIPPort);

        socket.send(packet);
    }

    // TODO Should have a timeout
    public static byte[] sendTCPMessage(byte[] msg, String ipAddr, int ipPort) throws IOException {
        Socket socket = new Socket(ipAddr, ipPort);
        DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
        DataInputStream istream = new DataInputStream(socket.getInputStream());

        ostream.write(msg);
        socket.shutdownOutput();
        byte[] response = istream.readAllBytes();

        istream.close();
        socket.close();

        return response;
    }
}
