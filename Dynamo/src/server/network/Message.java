package server.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Message {
    static public int MAX_MSG_SIZE = 10000;
    private final String multicastIpAddr;
    private final int multicastIPPort;

    public Message(String multicastIpAddr, int multicastIPPort) {
        this.multicastIpAddr = multicastIpAddr;
        this.multicastIPPort = multicastIPPort;
    }

    /**
     * Creates a Message according to our Message Structure
     * where the first line is the header, in the format
     * @return String with the message
     */
    private String buildMessage(String type, String action, String body) {
        StringBuilder sb = new StringBuilder();

        sb.append(type).append("\r\n");
        sb.append(action).append("\r\n");

        // empty line
        sb.append("\r\n");

        sb.append(body);

        return sb.toString();
    }

    /**
     * Creates a message and sends it by multicast
     */
    public void sendMulticast(String type, String action, String body) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(multicastIpAddr);

            byte[] msg = this.buildMessage(type, action, body).getBytes();
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

    /**
     * Message Structure
     * | type             |     (Request or Reply)
     * | action           |     ( join/leave/get/put/delete)
     * | CRLF             |
     * | Body             |
     *
     */
}
