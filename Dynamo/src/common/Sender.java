package common;

import server.Constants;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Sender {
    public static void sendMulticast(byte[] msg, String multicastIpAddr, int multicastIPPort) throws IOException {
        //noinspection resource
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(multicastIpAddr);

        DatagramPacket packet = new DatagramPacket(
                msg,
                msg.length,
                group,
                multicastIPPort);

        socket.send(packet);
    }

    public static byte[] sendTCPMessage(byte[] msg, String ipAddr, int ipPort) throws IOException {
        Socket socket = new Socket(ipAddr, ipPort);
        DataOutputStream ostream = new DataOutputStream(socket.getOutputStream());
        DataInputStream istream = new DataInputStream(socket.getInputStream());

        ostream.write(msg);
        socket.shutdownOutput();

        byte[] reply = new Message("REP", "timeout", null).toBytes();
        long maxResponseTime = System.currentTimeMillis() + Constants.timeoutTime;
        while (System.currentTimeMillis() < maxResponseTime) {
            // Node sent some data so it's alive
            if (istream.available() > 0) {
                reply = istream.readAllBytes();
                break;
            }

            // Otherwise, try again later
            try {
                Thread.sleep(Constants.tcpStepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        istream.close();
        socket.close();

        return reply;
    }
}
