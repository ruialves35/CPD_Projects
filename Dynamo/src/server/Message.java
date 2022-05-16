package server;

public class Message {
    private final String type;
    private final String action;

    private final String body;

    public Message(String type, String action, String body) {
        this.type = type;
        this.action = action;
        this.body = body;
    }

    /**
     * Creates a Message according to our Message Protocol
     * where the first line is the header, in the format
     * Type Action
     * (empty line)
     * Body
     * @return
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();

        // First line is
        sb.append(this.type).append(' ').append(this.action).append("\r\n");

        // empty line
        sb.append("\r\n");

        sb.append(body).append("\r\n");

        return body;
    }
}
