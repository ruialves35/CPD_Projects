package client;

import common.Message;
import common.Sender;
import common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        else
            client.keyValueOperation(nodeAP, operation, operand);
    }

    private void keyValueOperation(String nodeAP, String operation, String operand) {
        String[] nodeInfo = nodeAP.split(":", 2);
        String nodeIP = nodeInfo[0];
        int nodePort = Integer.parseInt(nodeInfo[1]);

        String key;
        byte[] file = null;

        if (operation.equals("put")) {
            try {
                Path path = Paths.get(operand);
                file = Files.readAllBytes(path);
                key = Utils.generateKey(file);
                System.out.println("Key=" + key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            key = operand;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(key.getBytes(StandardCharsets.UTF_8));
            if (file != null) {
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] body = out.toByteArray();

        Message msg = new Message("REQ", operation, body);
        Sender.sendTCPMessage(msg.toBytes(), nodeIP, nodePort);
    }

    private void membershipOperation(String nodeAP, String operation) {

    }
}
