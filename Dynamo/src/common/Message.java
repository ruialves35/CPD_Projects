package common;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Message Structure
 * | type             |     ( request or reply )
 * | action           |     ( join/leave/get/put/delete )
 * | IpAddress        |     ( nodeId )
 * | Port             |     ( Port )
 * | CRLF             |
 * | Body             |
 */
public class Message {
    static public int MAX_MSG_SIZE = 10000;
    private final String type;
    private final String action;
    private final String nodeId;

    private final int port;
    private final byte[] body;

    public Message(String type, String action, String nodeId, int port, byte[] body) {
        this.type = type;
        this.action = action;
        this.nodeId = nodeId;
        this.body = body;
        this.port = port;
    }

    public Message(byte[] bytes) throws IOException {
        final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        this.type = reader.readLine();
        this.action = reader.readLine();
        this.nodeId = reader.readLine();
        this.port = Integer.parseInt(reader.readLine());
        reader.readLine(); // last empty line

        int bodyOffset = type.length() + action.length() + nodeId.length() + 4 + 10; // 2 chars used for newlines per row on top

        //noinspection ResultOfMethodCallIgnored
        stream.skip(bodyOffset);
        this.body = stream.readAllBytes();
    }

    /**
     * Builds a Message according to our Message Structure
     * where the first line is the header, in the format
     * Type
     * Action
     * Node Id
     * Port
     * (empty line)
     * Body
     * @return Byte array with the message
     */
    public byte[] toBytes() throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(type).append("\r\n");
        sb.append(action).append("\r\n");
        sb.append(nodeId).append("\r\n");
        sb.append(port).append("\r\n");

        // empty line
        sb.append("\r\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        if (body != null) out.write(body);

        return out.toByteArray();
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public String getNodeId() { return nodeId; }

    public int getPort() {return port;}

    public byte[] getBody() {
        return body;
    }
}
