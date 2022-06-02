package server.cluster;

import common.Message;
import common.MessageTypes;
import common.Sender;
import common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ElectionService {
        public static void sendRequest(String nodeId, Node nextNode) {
                if (nextNode == null) return;

                System.out.printf("Sending Election Request to %s...\n", nextNode.getId());
                Path path = Paths.get(Utils.generateFolderPath(nodeId) + Utils.membershipLogFileName);

                try {
                        byte[] fileData = Files.readAllBytes(path);
                        String nodeIdLine = nodeId + Utils.newLine;

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        out.write(nodeIdLine.getBytes(StandardCharsets.UTF_8));
                        out.write(fileData);

                        Message electionMessage = new Message(MessageTypes.REQUEST.getCode(), MessageTypes.ELECTION_REQUEST.getCode(), out.toByteArray());
                        Sender.sendTCPMessage(electionMessage.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

        public static void propagateRequest(Message message, Node nextNode) {
                if (nextNode == null) return;

                System.out.printf("Propagating Election Request to %s...\n", nextNode.getId());
                try {
                        Sender.sendTCPMessage(message.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

}
