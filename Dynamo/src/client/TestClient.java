package client;

import common.Message;
import common.Sender;
import common.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestClient {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java TestClient <node_ap> <operation> [<opnd>]");
            System.exit(1);
        }

        // Either <IP address>:<port number> (TCP/UDP) or object's name (RMI)
        final String nodeAP = args[0];
        final String operation = args[1];
        final String operand = args.length == 3 ? args[2] : null;

        if (operand == null && operation.equals("put")) {
            System.out.println("The put operation requires the file pathname to be provided");
            System.exit(1);
        }

        if (operand == null && (operation.equals("get") || operation.equals("delete"))) {
            System.out.println("The " + operation + " requires a key to be provided");
            System.exit(1);
        }

        TestClient client = new TestClient();

            if (operation.equals("join") || operation.equals("leave"))
                client.membershipOperation(nodeAP, operation);
            else {
                try {
                    client.keyValueOperation(nodeAP, operation, operand);
                } catch (IOException e) {
                    // TODO Handle specific errors
                    System.out.println("Client sided error:");
                    e.printStackTrace();
                }
            }

    }

    private void keyValueOperation(String nodeAP, String operation, String operand) throws IOException {
        String[] nodeInfo = nodeAP.split(":", 2);
        String nodeIP = nodeInfo[0];
        int nodePort = Integer.parseInt(nodeInfo[1]);

        Message msg = buildKeyValueRequest(operand, operation);
        Message reply;
        do {
            reply = new Message(Sender.sendTCPMessage(msg.toBytes(), nodeIP, nodePort));
            System.out.println("Sent " + operation + " request to " + nodeIP + ":" + nodePort);

            if (reply.getAction().equals("redirect")) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(reply.getBody())));
                nodeIP = reader.readLine();
                nodePort = Integer.parseInt(reader.readLine());
                System.out.println("Redirecting to " + nodeIP + ":" + nodePort);
            } else {
                System.out.println("Received " + reply.getAction() + " reply");
            }
        } while (reply.getAction().equals("redirect"));

        if (operation.equals("get") && !reply.getAction().equals("error"))
            saveFile(reply.getBody());
    }

    private Message buildKeyValueRequest(String operand, String operation) throws IOException {
        String key;
        byte[] file = null;

        if (operation.equals("put")) {
            final Path path = Paths.get(operand);
            file = Files.readAllBytes(path);
            key = Utils.generateKey(file);
            System.out.println("Generated Key = " + key);
        } else {
            key = operand;
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(key.getBytes(StandardCharsets.UTF_8));
        if (file != null) {
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(file);
        }
        byte[] body = out.toByteArray();

        return new Message("REQ", operation, body);
    }

    private void saveFile(byte[] value) throws IOException {
        // TODO Where should we save the file?
        try (FileOutputStream fos = new FileOutputStream("file")) {
            fos.write(value);
        }
    }

    private void membershipOperation(String nodeAP, String operation) {

    }
}
