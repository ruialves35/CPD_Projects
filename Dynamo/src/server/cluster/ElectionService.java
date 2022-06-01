package server.cluster;

import common.Message;
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

                Path path = Paths.get(Utils.generateFolderPath(nodeId) + Utils.membershipLogFileName);

                try {
                        byte[] fileData = Files.readAllBytes(path);
                        String nodeIdLine = nodeId + Utils.newLine;

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        out.write(nodeIdLine.getBytes(StandardCharsets.UTF_8));
                        out.write(fileData);

                        Message electionMessage = new Message("REQ", "electionRequest", out.toByteArray());
                        Sender.sendTCPMessage(electionMessage.toBytes(), nextNode.getId(), nextNode.getPort());
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }
}
