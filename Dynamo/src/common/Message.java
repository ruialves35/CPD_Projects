package common;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Message Structure
 * | type             |     (Request or Reply)
 * | action           |     ( join/leave/get/put/delete)
 * | CRLF             |
 * | Body             |
 */
public class Message {
    static public int MAX_MSG_SIZE = 10000;
    private final String type;
    private final String action;
    private final byte[] body;

    public Message(String type, String action, byte[] body) {
        this.type = type;
        this.action = action;
        this.body = body;
    }

    public Message(byte[] bytes) {
        final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        try {
            this.type = reader.readLine();
            this.action = reader.readLine();
            reader.readLine(); // last empty line

            int bodyOffset = type.length() + action.length() + 6; // 6 chars used for newlines

            //noinspection ResultOfMethodCallIgnored
            stream.skip(bodyOffset);
            this.body = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a Message according to our Message Structure
     * where the first line is the header, in the format
     * Type
     * Action
     * (empty line)
     * Body
     * @return Byte array with the message
     */
    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();

        sb.append(type).append("\r\n");
        sb.append(action).append("\r\n");

        // empty line
        sb.append("\r\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.write(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public byte[] getBody() {
        return body;
    }
}
